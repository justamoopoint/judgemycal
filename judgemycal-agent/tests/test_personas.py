"""Persona layer: buddy is the safe default; every voice keeps the range visible."""
from judgemycal.personas import CHARTER, PERSONAS, PersonaEngine, persona_instruction


SAMPLE_EST = {
    "total_kcal": 640, "total_low": 540, "total_high": 750,
    "protein": 12.0, "carbs": 80.0, "fat": 20.0,
    "overall_confidence": "LOW", "lowest_confidence_item": "white rice",
}


class _Ctx:
    def __init__(self, state):
        self.state = state


def test_default_persona_is_buddy():
    instruction = persona_instruction(_Ctx({}))
    assert PERSONAS["buddy"]["voice"] in instruction


def test_unknown_persona_falls_back_to_buddy():
    instruction = persona_instruction(_Ctx({"persona": "drill_sergeant"}))
    assert PERSONAS["buddy"]["voice"] in instruction


def test_charter_present_for_every_persona():
    for persona in PERSONAS:
        assert CHARTER in persona_instruction(_Ctx({"persona": persona}))


def test_every_voice_keeps_the_range_visible():
    for persona in PERSONAS:
        text = PersonaEngine.react(persona, SAMPLE_EST)
        assert "540" in text and "750" in text, f"{persona} dropped the range"


def test_uncertain_estimates_name_the_shaky_item():
    for persona in PERSONAS:
        text = PersonaEngine.react(persona, SAMPLE_EST)
        assert "white rice" in text


def test_no_persona_uses_banned_terms():
    from judgemycal.safety import _BANNED
    high_conf = dict(SAMPLE_EST, overall_confidence="HIGH", lowest_confidence_item=None)
    for persona in PERSONAS:
        for est in (SAMPLE_EST, high_conf):
            text = PersonaEngine.react(persona, est).lower()
            for term in _BANNED:
                assert term not in text
