# JudgeMyCal — Agentic Application

The **brain** behind JudgeMyCal: a multi-agent system built on Google's Agent
Development Kit (ADK), with a real MCP server, capability sub-agents, a runtime
persona layer, and a **non-overridable safety floor** implemented as model
callbacks. The Android app in the companion repo is the client surface; this is the
agent that reasons and acts.

## Problem

Photo calorie tracking is easy to abandon (friction), easy to distrust (~15–25%
error, driven by portion estimation), and easy to make harmful (engagement-driven
shame). JudgeMyCal makes tracking effortless *and* safe: a personality you choose
does the nudging, honesty about uncertainty is a feature, and a safety floor the
persona **cannot override** keeps "judgy" from ever turning harmful.

## Why agents / why multi-agent

An orchestrator routes each request to a specialised capability agent; a persona
layer wraps the neutral result in voice; an independent guard inspects both the user
turn and the drafted reply and can break character. The separation is the point — a
single prompt can't hold the safety floor, because the floor works *precisely because*
it sits outside the model. **The persona proposes; the guard disposes.**

## Architecture

```
judgemycal (orchestrator, LlmAgent)
    ├─ cv_calorie       photo -> honest estimate; nutrition MCP tools
    ├─ health_shopping  goal-aligned swaps; never auto-purchases
    └─ workout_coach    adaptive sessions; respects rest, refuses pain
         ▲                         ▲
         │ persona voice (state)   │ MCP: verified nutrition DB (stdio server)
         └── SAFETY FLOOR: before/after-model callbacks on EVERY agent ──┘
                    memory bus = persona-agnostic session state
```

Guiding principle: *the harness, not the model.* The orchestration, the verified-data
reconciliation, and the guard do the heavy lifting; the model is a bounded component.

### Code map

| Concept | File |
| --- | --- |
| Orchestrator + wiring (`root_agent`) | `judgemycal/agent.py` |
| Capability agents | `judgemycal/capabilities/{cv_calorie,health_shopping,workout_coach}.py` |
| Persona layer (instruction provider + renderer) | `judgemycal/personas.py` |
| **Safety floor** (before/after-model callbacks) | `judgemycal/safety.py` |
| MCP server (verified nutrition) | `mcp_server/nutrition_server.py` |
| MCP toolset wiring | `judgemycal/mcp_tools.py` |
| Memory bus (session state) | `judgemycal/memory.py` |
| Pure estimator (mock vision + real reconciliation) | `judgemycal/estimator.py` |
| Agent Skills (capability contracts) | `skills/*/SKILL.md` |

## Course concepts demonstrated (rubric)

| Concept | Where | How |
| --- | --- | --- |
| **Multi-agent system (ADK)** | Code | `LlmAgent` orchestrator with three `sub_agents`, coordinated over shared session state. |
| **MCP Server** | Code | `mcp_server/nutrition_server.py` (FastMCP, stdio) exposes `lookup_nutrition` / `search_foods`; agents consume it via `McpToolset`. |
| **Agent Skills** | Code | Three `SKILL.md` capability contracts under `skills/`. |
| **Security features** | Code | Non-overridable safety floor as model callbacks on every agent; **no keys in code** (read from `.env`); shopping agent forbidden from auto-payment. |
| **Deployability** | Video/Code | Runs via `adk run` / `adk web`; offline demo needs no key or network. |

## Run it

### Offline demo — no API key, no network

```bash
python -m venv .venv && . .venv/bin/activate      # (Windows: .venv\Scripts\activate)
pip install -r requirements.txt
python demo_offline.py
```

This exercises the exact components the agents use — estimator, persona voices, and
the safety floor (including the break-character path) — so you can see the harness
work before wiring a model.

### Live agent — with a Gemini key

```bash
cp .env.example .env          # then set GOOGLE_API_KEY
adk run judgemycal            # terminal chat  (or: adk web)
# or:
python main.py                # simple REPL; type "/persona auntie|coach|buddy"
```

`adk run`/`adk web` auto-discover `root_agent` in `judgemycal/agent.py`.

## The safety floor (the demo's key moment)

`judgemycal/safety.py` attaches `before_model_callback` to every agent. If the user
turn contains distress markers, the callback **returns a response directly** — the
model is never called — so the persona is structurally unable to continue. Type
something like *"I feel like I should just stop eating"* and the companion drops
character and surfaces a calm, supportive message. A second `after_model_callback`
neutralises any punitive term that slips into a drafted reply.

## Security & privacy

- **No secrets in code** — the key is read from `.env` (gitignored) via `GOOGLE_API_KEY`.
- **Guard is structural**, not a prompt instruction that could be talked around.
- **No auto-spend** — the shopping agent may prepare a basket but is instructed never
  to complete payment without explicit per-purchase confirmation.

## Honest scope

The vision step in `estimator.py` is **mocked** (it selects a plausible meal); the
**nutrition reconciliation is real** (via the MCP-served verified DB), and the
confidence bands reflect genuine per-item uncertainty. Swapping in a live multimodal
identification call is the natural next step; the persona-neutral contract between
capability and orchestrator stays identical. The safety floor here is the deterministic
(L1) tier; the full product adds an independent semantic (L2) auditor.

## Project structure

```
judgemycal-agent/
├── judgemycal/
│   ├── agent.py            # root_agent (orchestrator)
│   ├── personas.py         # persona voices + instruction provider
│   ├── safety.py           # non-overridable floor (callbacks)
│   ├── memory.py           # persona-agnostic memory bus
│   ├── estimator.py        # mock vision + real reconciliation
│   ├── nutrition_db.py     # verified nutrition table
│   ├── mcp_tools.py        # McpToolset wiring
│   └── capabilities/       # cv_calorie, health_shopping, workout_coach
├── mcp_server/nutrition_server.py   # FastMCP stdio server
├── skills/                 # SKILL.md capability contracts
├── demo_offline.py         # runs with no key
├── main.py                 # live ADK Runner CLI
├── requirements.txt
└── .env.example
```
