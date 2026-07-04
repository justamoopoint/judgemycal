"""Wires the nutrition MCP server into ADK as a toolset the agents can call."""
import os
import sys

from google.adk.tools.mcp_tool.mcp_toolset import (
    McpToolset,
    StdioConnectionParams,
    StdioServerParameters,
)

_SERVER = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "mcp_server", "nutrition_server.py",
)


def nutrition_toolset() -> McpToolset:
    """A fresh McpToolset that launches the nutrition MCP server over stdio.

    Build one per agent that needs it (each gets its own server subprocess).
    """
    return McpToolset(
        connection_params=StdioConnectionParams(
            server_params=StdioServerParameters(
                command=sys.executable,
                args=[_SERVER],
            )
        )
    )
