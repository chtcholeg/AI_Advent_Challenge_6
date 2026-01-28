"""Telegram MCP tool definitions.

Each tool reads data from public Telegram channels via Telethon:
- name: unique identifier used in tools/call
- description: shown to AI for function selection
- input_schema: JSON Schema describing accepted parameters
- execute(): async handler returning ToolResult
"""

import logging

from telegram_client import TelegramChannelClient

logger = logging.getLogger(__name__)


class ToolResult:
    """Wraps tool output with error flag for MCP protocol."""

    def __init__(self, content: str, is_error: bool = False):
        self.content = content
        self.is_error = is_error


class TelegramTool:
    """Base class for all Telegram MCP tools."""

    name: str = ""
    description: str = ""
    input_schema: dict = {}

    def __init__(self, client: TelegramChannelClient):
        self.client = client

    async def execute(self, arguments: dict) -> ToolResult:
        raise NotImplementedError


# ---------------------------------------------------------------------------
# Tool: get_channel_messages
# ---------------------------------------------------------------------------

class GetChannelMessagesTool(TelegramTool):
    name = "get_channel_messages"
    description = (
        "Get the latest messages from a public Telegram channel. "
        "Returns message text, date, view count, and ID. "
        "Channel must be public (have a @username). "
        "By default returns the 5 most recent messages."
    )
    input_schema = {
        "type": "object",
        "properties": {
            "channel": {
                "type": "string",
                "description": "Telegram channel username (e.g. 'durov' or '@durov')",
            },
            "count": {
                "type": "integer",
                "description": "Number of messages to retrieve (1-100, default: 5)",
            },
        },
        "required": ["channel"],
    }

    async def execute(self, arguments: dict) -> ToolResult:
        channel = arguments.get("channel", "").strip()
        if not channel:
            return ToolResult("Missing required parameter: channel", is_error=True)

        count = arguments.get("count", 5)
        count = max(1, min(100, int(count)))

        try:
            messages = await self.client.get_messages(channel, limit=count)
            channel_clean = channel.lstrip("@")

            if not messages:
                return ToolResult(f"No text messages found in @{channel_clean}.")

            lines = [
                f"Latest {len(messages)} messages from @{channel_clean}:",
                "=" * 50,
                "",
            ]

            for i, msg in enumerate(messages, 1):
                lines.append(f"[{i}] ID: {msg['id']}  |  {msg['date']}")
                if msg.get("views"):
                    lines.append(f"    Views: {msg['views']}")
                if msg.get("has_media"):
                    lines.append(f"    [has media attachment]")
                if msg.get("reply_to_id"):
                    lines.append(f"    Reply to: #{msg['reply_to_id']}")
                text = msg["text"]
                if len(text) > 500:
                    text = text[:500] + "\n    ... [truncated]"
                lines.append(f"    {text}")
                lines.append("")

            return ToolResult("\n".join(lines))
        except Exception as e:
            logger.error(f"GetChannelMessagesTool error: {e}")
            return ToolResult(
                f"Failed to get messages from @{channel.lstrip('@')}: {e}",
                is_error=True,
            )


# ---------------------------------------------------------------------------
# Tool: get_channel_info
# ---------------------------------------------------------------------------

class GetChannelInfoTool(TelegramTool):
    name = "get_channel_info"
    description = (
        "Get information about a public Telegram channel: "
        "title, description, member count, creation date, and channel type. "
        "Channel must be public (have a @username)."
    )
    input_schema = {
        "type": "object",
        "properties": {
            "channel": {
                "type": "string",
                "description": "Telegram channel username (e.g. 'durov' or '@durov')",
            },
        },
        "required": ["channel"],
    }

    async def execute(self, arguments: dict) -> ToolResult:
        channel = arguments.get("channel", "").strip()
        if not channel:
            return ToolResult("Missing required parameter: channel", is_error=True)

        try:
            info = await self.client.get_channel_info(channel)

            channel_type = "Supergroup" if info.get("is_supergroup") else "Channel"

            lines = [
                f"{channel_type}: @{info['username']}",
                "=" * 50,
                "",
                f"Title: {info['title']}",
            ]
            if info.get("description"):
                lines.append(f"Description: {info['description']}")
            lines.append("")

            if info.get("members_count") is not None:
                lines.append(f"Members: {info['members_count']:,}")
            if info.get("verified"):
                lines.append("Verified: Yes")
            lines.append(
                "Type: Broadcast (read-only)"
                if info.get("is_broadcast")
                else "Type: Interactive (members can post)"
            )
            if info.get("created"):
                lines.append(f"Created: {info['created']}")

            return ToolResult("\n".join(lines))
        except Exception as e:
            logger.error(f"GetChannelInfoTool error: {e}")
            return ToolResult(
                f"Failed to get channel info for @{channel.lstrip('@')}: {e}",
                is_error=True,
            )


# ---------------------------------------------------------------------------
# Registry
# ---------------------------------------------------------------------------

def get_all_tools(client: TelegramChannelClient) -> list[TelegramTool]:
    """Return all registered Telegram MCP tools."""
    return [
        GetChannelMessagesTool(client),
        GetChannelInfoTool(client),
    ]
