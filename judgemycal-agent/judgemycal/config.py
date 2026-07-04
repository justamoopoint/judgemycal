"""Central configuration."""
import os

# The model the agents run on. Override with JUDGEMYCAL_MODEL.
# Any Gemini model string understood by google-genai works here.
MODEL = os.environ.get("JUDGEMYCAL_MODEL", "gemini-2.0-flash")
