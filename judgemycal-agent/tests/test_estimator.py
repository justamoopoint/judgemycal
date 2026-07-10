"""Estimator invariants: honest bands, lowest-confidence-wins, real reconciliation."""
from judgemycal import estimator, nutrition_db


def _estimates(n=25):
    return [estimator.estimate(image_path=f"photo_{i}.jpg") for i in range(n)]


def test_every_estimate_is_a_range_never_a_point():
    for est in _estimates():
        assert est["total_low"] < est["total_kcal"] < est["total_high"]


def test_totals_are_item_sums():
    for est in _estimates():
        assert est["total_kcal"] == sum(i["kcal"] for i in est["items"])
        assert est["total_low"] == sum(i["kcal_low"] for i in est["items"])
        assert est["total_high"] == sum(i["kcal_high"] for i in est["items"])


def test_band_width_matches_item_confidence():
    for est in _estimates():
        for item in est["items"]:
            spread = estimator._BAND[item["confidence"]]
            assert item["kcal_low"] == round(item["kcal"] * (1 - spread))
            assert item["kcal_high"] == round(item["kcal"] * (1 + spread))


def test_overall_confidence_is_the_lowest_items_never_an_average():
    order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}
    for est in _estimates():
        worst = max(order[i["confidence"]] for i in est["items"])
        assert order[est["overall_confidence"]] == worst


def test_low_confidence_meal_names_its_shakiest_item():
    for est in _estimates():
        if est["overall_confidence"] == "LOW":
            names = {i["name"] for i in est["items"] if i["confidence"] == "LOW"}
            assert est["lowest_confidence_item"] in names


def test_items_reconciled_against_verified_db():
    est = estimator.estimate(image_path="any.jpg")
    for item in est["items"]:
        row = nutrition_db.lookup(item["name"], item["grams"])
        assert row["kcal"] == item["kcal"]
