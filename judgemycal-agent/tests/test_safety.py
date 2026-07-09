"""The safety floor must never regress: distress detection, punitive-term
neutralisation, and the structural break-character short-circuit."""
import pytest

from judgemycal import safety


DISTRESS_UTTERANCES = [
    "I feel like I should just stop eating",
    "I've been starving myself all week",
    "haven't eaten since Tuesday",
    "I'm skipping meals to hit my goal",
    "I made myself sick after dinner",
    "I hate my body",
    "I'm so fat and worthless",
    "I want to disappear",
    "maybe I should punish myself with a workout",
]

BENIGN_UTTERANCES = [
    "what's in this meal?",
    "I had chicken and rice for lunch",
    "build me a 30 minute strength workout",
    "swap suggestions for my shopping basket please",
    "how many calories in a banana",
    "I ate a huge burger and fries today, oops",
]


@pytest.mark.parametrize("text", DISTRESS_UTTERANCES)
def test_distress_detected(text):
    assert safety.is_distress(text)


@pytest.mark.parametrize("text", BENIGN_UTTERANCES)
def test_benign_not_flagged(text):
    assert not safety.is_distress(text)


def test_empty_and_none_are_safe():
    assert not safety.is_distress("")
    assert not safety.is_distress(None)


def test_govern_text_neutralises_banned_terms():
    out = safety.govern_text("Don't be lazy, that plate is disgusting.")
    assert "lazy" not in out.lower()
    assert "disgusting" not in out.lower()


def test_govern_text_leaves_clean_text_alone():
    text = "Nice plate! ~640 kcal (540-750). You logged it, that's the win."
    assert safety.govern_text(text) == text


class _FakePart:
    def __init__(self, text):
        self.text = text


class _FakeContent:
    def __init__(self, text):
        self.role = "user"
        self.parts = [_FakePart(text)]


class _FakeCallbackContext:
    def __init__(self, text):
        self.user_content = _FakeContent(text)


class _FakeRequest:
    contents = []


def test_before_model_callback_short_circuits_on_distress():
    """The structural guarantee: the model is never consulted on distress."""
    ctx = _FakeCallbackContext("I feel like I should just stop eating")
    response = safety.before_model_callback(ctx, _FakeRequest())
    assert response is not None
    text = "".join(p.text or "" for p in response.content.parts)
    assert text == safety.SUPPORT_MESSAGE


def test_before_model_callback_passes_benign_turns():
    ctx = _FakeCallbackContext("what's in this meal?")
    assert safety.before_model_callback(ctx, _FakeRequest()) is None


def test_after_model_callback_neutralises_drafted_reply():
    from google.adk.models import LlmResponse
    from google.genai import types

    drafted = LlmResponse(content=types.Content(
        role="model", parts=[types.Part(text="That was a lazy choice.")]))
    fixed = safety.after_model_callback(None, drafted)
    assert fixed is not None
    assert "lazy" not in fixed.content.parts[0].text.lower()


def test_after_model_callback_returns_none_when_clean():
    from google.adk.models import LlmResponse
    from google.genai import types

    drafted = LlmResponse(content=types.Content(
        role="model", parts=[types.Part(text="Great logging streak!")]))
    assert safety.after_model_callback(None, drafted) is None
