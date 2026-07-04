"""Live CLI runner for the JudgeMyCal agent (requires a Gemini API key).

Set GOOGLE_API_KEY (see .env.example), then:
    python main.py
Or use the ADK tooling directly:
    adk run judgemycal      # terminal chat
    adk web                 # local web UI

Pick a persona at any time by typing:  /persona auntie|coach|buddy
"""
import asyncio

from google.adk.runners import InMemoryRunner
from google.genai import types

from judgemycal.agent import root_agent

APP = "judgemycal"
USER = "local-user"


async def main():
    runner = InMemoryRunner(agent=root_agent, app_name=APP)
    session = await runner.session_service.create_session(
        app_name=APP, user_id=USER, state={"persona": "buddy"}
    )

    print("JudgeMyCal is ready. Type a message, '/persona <name>' to switch, or 'quit'.")
    while True:
        try:
            text = input("\nyou > ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not text or text.lower() in {"quit", "exit"}:
            break

        if text.startswith("/persona"):
            name = text.split(maxsplit=1)[1].strip().lower() if " " in text else "buddy"
            # Update the persona-agnostic memory bus for this session.
            s = await runner.session_service.get_session(
                app_name=APP, user_id=USER, session_id=session.id
            )
            s.state["persona"] = name
            print(f"[persona set to {name}]")
            continue

        msg = types.Content(role="user", parts=[types.Part(text=text)])
        async for event in runner.run_async(
            user_id=USER, session_id=session.id, new_message=msg
        ):
            if event.is_final_response() and event.content and event.content.parts:
                out = "".join(p.text or "" for p in event.content.parts)
                if out:
                    print(f"\njudgemycal > {out}")


if __name__ == "__main__":
    asyncio.run(main())
