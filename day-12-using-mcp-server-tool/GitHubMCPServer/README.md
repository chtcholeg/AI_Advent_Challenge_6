# GitHub MCP Server

A lightweight MCP (Model Context Protocol) server written in Python that exposes GitHub repository data as callable tools for AI assistants.

The server communicates via HTTP using SSE (Server-Sent Events) transport and JSON-RPC 2.0 framing — the standard MCP wire format.

---

## Quick Start

### 1. Install dependencies

```bash
pip install -r requirements.txt
```

### 2. Run without authentication (fastest start)

```bash
python main.py --no-auth
```

The server starts on `http://localhost:8000`. No API key is needed — useful for local development and testing.

### 3. Run with authentication

```bash
export MCP_API_KEY="my-secret-key"
python main.py
```

All MCP endpoints (`/sse`, `/message`, `/tools`) now require an `X-API-Key: my-secret-key` header. Health check (`/health`) and docs (`/docs`) remain public.

---

## Two Independent Auth Layers

| Layer | Controls | How to configure |
|-------|----------|------------------|
| **MCP Server Auth** | Who can call your MCP server | `MCP_API_KEY` env var + `X-API-Key` header |
| **GitHub API Auth** | Rate limits for GitHub API calls | `GITHUB_TOKEN` env var (optional) |

### MCP Server Auth

- **Enabled (default):** set `MCP_API_KEY` in the environment. Clients must pass the key via `X-API-Key` header or `?api_key=` query param.
- **Disabled:** start with `--no-auth` flag. No key required. Suitable for local/dev use only.

### GitHub API Auth

- **Without token:** GitHub allows 60 unauthenticated requests per hour.
- **With token:** set `GITHUB_TOKEN` to a [personal access token](https://github.com/settings/tokens). Limit rises to 5000 requests per hour.

The token is only used for calling the GitHub API — it is never exposed to MCP clients.

---

## Configuration

All settings can be overridden via environment variables or CLI flags.

| Env var | Default | Description |
|---------|---------|-------------|
| `MCP_API_KEY` | — | Server API key (required unless `--no-auth`) |
| `GITHUB_TOKEN` | — | GitHub PAT for higher rate limits |
| `HOST` | `0.0.0.0` | Bind address |
| `PORT` | `8000` | Bind port |

### CLI flags

| Flag | Description |
|------|-------------|
| `--no-auth` | Disable MCP API key authentication |
| `--host <addr>` | Override bind address |
| `--port <n>` | Override bind port |

### Example `.env`

Copy `.env.example` and fill in your values:

```bash
cp .env.example .env
# edit .env
source .env   # or use dotenv loader
python main.py
```

---

## Available Tools

### `get_repo_info`

Get general repository metadata.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |

Returns: description, stars, forks, watchers, open issues, language, license, default branch, topics, dates.

### `get_repo_branches`

List branches of a repository.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |
| `per_page` | no | Number of branches (1–100, default 30) |

Returns: branch names, protection status, latest commit SHA.

### `get_repo_tags`

List tags (releases) of a repository.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |
| `per_page` | no | Number of tags (1–100, default 30) |

Returns: tag names and associated commit SHAs.

### `get_readme`

Fetch and decode the README of a repository.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |
| `ref` | no | Branch, tag, or commit (default: default branch) |

Returns: full README text content.

### `get_repo_contributors`

Get the top contributors to a repository.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |
| `per_page` | no | Number of contributors (1–100, default 10) |

Returns: username, contribution count, profile URL.

### `get_repo_commits`

Fetch recent commits, optionally filtered by branch.

| Parameter | Required | Description |
|-----------|----------|-------------|
| `owner` | yes | GitHub username or organization |
| `repo` | yes | Repository name |
| `branch` | no | Branch name (default: default branch) |
| `per_page` | no | Number of commits (1–100, default 10) |

Returns: short SHA, commit message (first line), author name, date.

---

## MCP Protocol Details

### Transport

SSE over HTTP. Two endpoints form the transport:

1. **`GET /sse`** — client connects and receives a stream of events. The first event (`event: endpoint`) tells the client the message URL:
   ```
   event: endpoint
   data: /message?sessionId=<uuid>
   ```

2. **`POST /message?sessionId=<id>`** — client sends a JSON-RPC request. The server processes it, pushes the response into the SSE stream, and also returns it in the POST response body.

### JSON-RPC Methods

| Method | Direction | Description |
|--------|-----------|-------------|
| `initialize` | client → server | Handshake; server returns protocol version and capabilities |
| `initialized` | client → server | Acknowledgement (server returns `{}`) |
| `tools/list` | client → server | Server returns array of tool definitions |
| `tools/call` | client → server | Execute a tool; server returns content + isError flag |
| `ping` | client → server | Keepalive; server returns `{}` |

### Example: discover tools

```bash
curl -s -X POST http://localhost:8000/message?sessionId=<id> \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

### Example: call a tool

```bash
curl -s -X POST http://localhost:8000/message?sessionId=<id> \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0","id":2,"method":"tools/call",
    "params":{
      "name":"get_repo_info",
      "arguments":{"owner":"JetBrains","repo":"kotlin"}
    }
  }'
```

---

## Testing with curl

### Health check (no auth needed)

```bash
curl http://localhost:8000/health
```

### List available tools

```bash
# Without auth
curl http://localhost:8000/tools

# With auth
curl -H "X-API-Key: my-secret-key" http://localhost:8000/tools
```

### Full MCP session (manual)

```bash
# 1. Open SSE connection (in one terminal)
curl -s http://localhost:8000/sse

# 2. Send initialize (in another terminal, using sessionId from step 1)
SESSION_ID="<uuid-from-sse-output>"
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize"}'

# 3. List tools
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# 4. Call a tool
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_repo_info","arguments":{"owner":"microsoft","repo":"vscode"}}}'
```

---

## Connecting from the Kotlin App

In the main GigaChat app, add the server via the MCP Management screen:

| Field | Value |
|-------|-------|
| Name | GitHub MCP |
| Type | HTTP |
| URL | `http://localhost:8000/sse` |
| API Key | *(your MCP_API_KEY, or leave empty if `--no-auth`)* |

The app will auto-discover the 6 GitHub tools and make them available for AI function calling.

---

## Project Structure

```
GitHubMCPServer/
├── main.py           # FastAPI app setup, CLI parsing, server start
├── config.py         # Configuration from env vars
├── github_client.py  # Async HTTP client for GitHub REST API
├── mcp_protocol.py   # JSON-RPC 2.0 / MCP protocol handler
├── sse_transport.py  # SSE session management and route registration
├── tools.py          # All 6 GitHub tool implementations
├── requirements.txt  # Python dependencies (fastapi, uvicorn, httpx)
├── .env.example      # Template for environment variables
└── README.md         # This file
```

---

## Requirements

- Python 3.10+
- No external services required beyond GitHub's public API (optional token for higher limits)

## Dependencies

| Package | Purpose |
|---------|---------|
| `fastapi` | HTTP framework (ASGI) |
| `uvicorn` | ASGI server |
| `httpx` | Async HTTP client for GitHub API |

All three are lightweight with minimal transitive dependencies.
