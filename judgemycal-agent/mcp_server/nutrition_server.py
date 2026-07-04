"""A stdio MCP server exposing the verified nutrition database as tools.

Run standalone with:  python mcp_server/nutrition_server.py
The ADK `cv_calorie` / `health_shopping` agents connect to it via McpToolset
(see judgemycal/mcp_tools.py). This is the project's "MCP Server" concept, in code.
"""
import os
import sys

# Make the judgemycal package importable when run as a standalone script.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mcp.server.fastmcp import FastMCP
from judgemycal import nutrition_db

mcp = FastMCP("judgemycal-nutrition")


@mcp.tool()
def lookup_nutrition(food: str, grams: float) -> dict:
    """Return verified calories and macros for a given food and portion in grams.

    Use this to reconcile any estimated food item against ground-truth data rather
    than trusting the model's own recall. `matched=false` means the food is unknown
    and the estimate should be treated as low confidence.
    """
    return nutrition_db.lookup(food, grams)


@mcp.tool()
def search_foods(query: str) -> list[str]:
    """Return known foods in the database relevant to a search query."""
    return nutrition_db.search(query)


if __name__ == "__main__":
    mcp.run(transport="stdio")
