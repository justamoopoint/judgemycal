"""Offline demo — runs with NO API key and NO network.

It exercises the exact deterministic components the ADK agents use: the nutrition
database, the estimator (mock vision + real reconciliation), the persona voices, and
the safety floor. Use it to show the harness works even before a model is wired in.

    python demo_offline.py
"""
from judgemycal import estimator, safety
from judgemycal.personas import PersonaEngine
from judgemycal.memory import add_meal, daily_total


def rule(title):
    print("\n" + "=" * 68 + f"\n{title}\n" + "=" * 68)


def main():
    state: dict = {"persona": "buddy", "meal_log": []}

    rule("1) cv-calorie: mock vision + REAL nutrition reconciliation")
    est = estimator.estimate(image_path="demo/plate.jpg")
    for it in est["items"]:
        print(f"  - {it['name']:<28} {it['grams']:>4}g  "
              f"{it['kcal']:>4} kcal ({it['kcal_low']}-{it['kcal_high']})  "
              f"[{it['confidence']}]")
    print(f"  TOTAL: ~{est['total_kcal']} kcal  range {est['total_low']}-{est['total_high']}"
          f"   overall={est['overall_confidence']}"
          f"   least-sure={est['lowest_confidence_item']}")
    add_meal(state, {"name": est["items"][0]["name"], "kcal": est["total_kcal"]})
    print(f"  logged -> daily total now {daily_total(state)} kcal")

    rule("2) Persona layer: same numbers, three voices")
    for p in ("auntie", "coach", "buddy"):
        print(f"\n  [{p}]  {PersonaEngine.react(p, est)}")

    rule("3) Safety floor: benign input passes")
    msg = "just had lunch, how am I doing today?"
    print(f"  user: {msg}\n  distress? {safety.is_distress(msg)}  -> proceed in persona voice")

    rule("4) Safety floor: distress BREAKS CHARACTER (structural)")
    msg = "honestly I feel like I should just stop eating for a few days"
    print(f"  user: {msg}\n  distress? {safety.is_distress(msg)}")
    if safety.is_distress(msg):
        print("  --- persona suppressed; support message returned instead ---")
        print("  " + safety.SUPPORT_MESSAGE)

    rule("5) Output backstop: punitive term neutralised")
    drafted = "You were so lazy today."
    print(f"  drafted: {drafted!r}\n  governed: {safety.govern_text(drafted)!r}")


if __name__ == "__main__":
    main()
