"""The non-overridable safety floor.

Implemented as ADK model callbacks rather than prompt text, so no persona (and no
jailbreak of a persona) can talk past it:

  * `before_model_callback` inspects the incoming user turn. On a distress signal it
    RETURNS an LlmResponse, which short-circuits the model call entirely — the LLM is
    never even asked. That is the structural "break character" guarantee.
  * `after_model_callback` inspects the model's drafted reply and neutralises any
    punitive term that slipped through (defence in depth).

The rule functions at the top are pure (no ADK/genai imports), so the offline demo
can reuse them. The ADK/genai types are imported lazily inside the callbacks.
"""
from __future__ import annotations
import re
from typing import Optional

# ---- pure rules (importable anywhere) -------------------------------------

_DISTRESS = [re.compile(p, re.I) for p in [
    r"\bstarv(e|ing)\b", r"haven'?t eaten", r"stop(ped)? eating", r"skip(ping)? meals",
    r"\bnot eating\b", r"barely eat", r"punish myself",
    r"purge", r"purging", r"throw(ing)? up", r"ma(?:k|d)e myself sick", r"vomit",
    r"hate my body", r"hate how i look", r"i'?m disgusting", r"i'?m so fat", r"worthless",
    r"hurt myself", r"want to disappear", r"don'?t want to be here", r"end it all",
    r"no reason to live",
]]

_BANNED = ["disgusting", "lazy", "failure", "greedy", "worthless"]

SUPPORT_MESSAGE = (
    "I'm going to step out of character for a moment, because what you said matters more "
    "than any number. You deserve real support from someone who can genuinely help \u2014 "
    "please consider reaching out to a doctor, a mental-health professional, or someone "
    "you trust. If you might be in immediate danger, contact your local emergency services "
    "right away. I can't be that support on my own, but I didn't want to just carry on as "
    "if you hadn't said it."
)


def is_distress(text: str) -> bool:
    return any(p.search(text or "") for p in _DISTRESS)


def govern_text(text: str) -> str:
    out = text or ""
    for term in _BANNED:
        out = re.sub(rf"\b{term}\b", "\u2014", out, flags=re.I)
    return out


# ---- ADK callbacks --------------------------------------------------------

def _latest_user_text(callback_context, llm_request) -> str:
    parts_text = []
    content = getattr(callback_context, "user_content", None)
    if content is None:
        contents = getattr(llm_request, "contents", None) or []
        for c in reversed(contents):
            if getattr(c, "role", None) == "user":
                content = c
                break
    if content is not None:
        for part in (getattr(content, "parts", None) or []):
            t = getattr(part, "text", None)
            if t:
                parts_text.append(t)
    return " ".join(parts_text)


def before_model_callback(callback_context, llm_request) -> Optional[object]:
    """Break character before the model is called if the user is in distress."""
    text = _latest_user_text(callback_context, llm_request)
    if is_distress(text):
        from google.adk.models import LlmResponse
        from google.genai import types
        return LlmResponse(
            content=types.Content(role="model",
                                  parts=[types.Part(text=SUPPORT_MESSAGE)])
        )
    return None  # otherwise proceed to the model as normal


def after_model_callback(callback_context, llm_response) -> Optional[object]:
    """Neutralise any punitive term in the model's drafted reply (backstop)."""
    content = getattr(llm_response, "content", None)
    if content is None:
        return None
    changed = False
    new_parts = []
    from google.genai import types
    for part in (getattr(content, "parts", None) or []):
        t = getattr(part, "text", None)
        if t:
            g = govern_text(t)
            if g != t:
                changed = True
            new_parts.append(types.Part(text=g))
        else:
            new_parts.append(part)
    if not changed:
        return None
    from google.adk.models import LlmResponse
    return LlmResponse(content=types.Content(role="model", parts=new_parts))
