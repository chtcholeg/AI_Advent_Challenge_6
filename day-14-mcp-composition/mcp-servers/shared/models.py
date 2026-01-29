"""Common data models for MCP tools."""

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class ToolResult:
    """Wraps tool output with error flag for MCP protocol."""
    content: str
    is_error: bool = False


class BaseTool(ABC):
    """Abstract base class for MCP tools.

    Subclasses must define:
    - name: unique tool identifier
    - description: human-readable description
    - input_schema: JSON Schema for arguments
    - execute(): async method to run the tool
    """

    name: str = ""
    description: str = ""
    input_schema: dict = {}

    @abstractmethod
    async def execute(self, arguments: dict) -> ToolResult:
        """Execute the tool with given arguments."""
        pass
