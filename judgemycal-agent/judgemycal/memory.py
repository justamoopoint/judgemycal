"""Persona-agnostic memory bus.

Thin helpers over the ADK session `state` dict (also usable with a plain dict in
the offline demo). Memory is neutral to persona, so switching companion never
changes the underlying data.
"""
from __future__ import annotations
from typing import Any

PERSONA_KEY = "persona"
LOG_KEY = "meal_log"


def get_persona(state: dict[str, Any]) -> str:
    return state.get(PERSONA_KEY, "buddy")


def set_persona(state: dict[str, Any], persona: str) -> None:
    state[PERSONA_KEY] = persona.lower()


def get_log(state: dict[str, Any]) -> list[dict]:
    return state.get(LOG_KEY, [])


def add_meal(state: dict[str, Any], meal: dict) -> list[dict]:
    log = list(state.get(LOG_KEY, []))
    log.append(meal)
    state[LOG_KEY] = log
    return log


def daily_total(state: dict[str, Any]) -> int:
    return sum(int(m.get("kcal", 0)) for m in state.get(LOG_KEY, []))
