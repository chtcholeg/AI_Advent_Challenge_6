"""Quick smoke test for the GitHub MCP server.

Run the server first:
    python main.py --no-auth --port 8000

Then run this script:
    python test_server.py [--port 8000]

Tests: SSE session creation, MCP initialize, tools/list, and one tool call.
"""

import asyncio
import json
import sys
import argparse

import httpx


async def test(base_url: str):
    async with httpx.AsyncClient(timeout=30.0) as client:
        session_id = None
        session_ready = asyncio.Event()

        async def keep_sse_alive():
            """Read SSE stream in background to maintain the connection."""
            nonlocal session_id
            async with client.stream("GET", f"{base_url}/sse") as resp:
                async for line in resp.aiter_lines():
                    if line.startswith("data:") and session_id is None:
                        session_id = line.split("sessionId=", 1)[1].strip()
                        session_ready.set()
                    # Continue reading to keep connection alive

        # Start SSE reader in background
        sse_task = asyncio.create_task(keep_sse_alive())
        await asyncio.wait_for(session_ready.wait(), timeout=5.0)

        print(f"[1] SSE session established: {session_id}")
        msg_url = f"{base_url}/message?sessionId={session_id}"

        # 2. Initialize
        print("[2] Sending initialize...")
        r = await client.post(msg_url, json={
            "jsonrpc": "2.0", "id": 1, "method": "initialize"
        })
        init = r.json()
        assert init.get("result", {}).get("protocolVersion") == "2024-11-05", f"Bad init: {init}"
        print(f"     Protocol: {init['result']['protocolVersion']}")
        print(f"     Server:   {init['result']['serverInfo']}")

        # 3. Tools list
        print("[3] Listing tools...")
        r = await client.post(msg_url, json={
            "jsonrpc": "2.0", "id": 2, "method": "tools/list"
        })
        tools = r.json()["result"]["tools"]
        print(f"     Found {len(tools)} tool(s):")
        for t in tools:
            print(f"       - {t['name']}")

        # 4. Call get_repo_info
        print("[4] Calling get_repo_info(owner=JetBrains, repo=kotlin)...")
        r = await client.post(msg_url, json={
            "jsonrpc": "2.0", "id": 3, "method": "tools/call",
            "params": {
                "name": "get_repo_info",
                "arguments": {"owner": "JetBrains", "repo": "kotlin"},
            },
        })
        call_result = r.json()["result"]
        is_err = call_result.get("isError", False)
        print(f"     isError: {is_err}")
        text = call_result["content"][0]["text"]
        for line in text.split("\n")[:12]:
            print(f"     | {line}")

        # 5. Ping
        print("[5] Ping...")
        r = await client.post(msg_url, json={
            "jsonrpc": "2.0", "id": 4, "method": "ping"
        })
        assert r.json()["result"] == {}, f"Bad ping: {r.json()}"
        print("     OK")

        # Cleanup
        sse_task.cancel()
        try:
            await sse_task
        except asyncio.CancelledError:
            pass

        if is_err:
            print("\nTools executed but returned error (possibly GitHub rate limit).")
        else:
            print("\nAll tests passed.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    asyncio.run(test(f"http://localhost:{args.port}"))
