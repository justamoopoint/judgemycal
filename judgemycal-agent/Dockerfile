# Production image for the JudgeMyCal agent API (Cloud Run).
# The layout mirrors the repo so the MCP nutrition server resolves the same
# way it does locally (a stdio subprocess next to the judgemycal package).
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# The agent package and its sibling mcp_server/ live together under agents/
# so judgemycal/mcp_tools.py resolves the stdio server exactly as it does in
# the repo (../mcp_server/nutrition_server.py relative to the package).
COPY judgemycal/ agents/judgemycal/
COPY mcp_server/ agents/mcp_server/
COPY server/ server/

# Non-root user (Cloud Run best practice).
RUN useradd -m appuser
USER appuser

ENV AGENTS_DIR=/app/agents

# Cloud Run injects PORT; default for local docker run.
CMD ["sh", "-c", "uvicorn server.app:create_app --factory --host 0.0.0.0 --port ${PORT:-8080}"]
