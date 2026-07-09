package com.judgemycal.app.domain

import kotlin.math.roundToInt

/**
 * On-device copy of the verified nutrition table (values per 100 g), ported from
 * the backend's `nutrition_db.py`. Used only by the offline fallback engine —
 * when the backend is reachable, reconciliation happens server-side against the
 * MCP-served table.
 */
object NutritionDb {

    data class Row(
        val food: String,
        val matched: Boolean,
        val grams: Int,
        val kcal: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
    )

    // name -> kcal, protein, carbs, fat per 100 g
    private val TABLE: Map<String, DoubleArray> = mapOf(
        "grilled chicken breast" to doubleArrayOf(165.0, 31.0, 0.0, 3.6),
        "grilled chicken thigh" to doubleArrayOf(209.0, 26.0, 0.0, 11.0),
        "chicken curry" to doubleArrayOf(180.0, 14.0, 6.0, 11.0),
        "white rice" to doubleArrayOf(130.0, 2.7, 28.0, 0.3),
        "basmati rice" to doubleArrayOf(121.0, 3.0, 25.0, 0.4),
        "brown rice" to doubleArrayOf(112.0, 2.6, 24.0, 0.9),
        "naan" to doubleArrayOf(310.0, 9.0, 50.0, 6.0),
        "roti" to doubleArrayOf(297.0, 11.0, 46.0, 7.0),
        "dal" to doubleArrayOf(116.0, 9.0, 20.0, 0.4),
        "paneer" to doubleArrayOf(265.0, 18.0, 1.2, 20.0),
        "mixed salad" to doubleArrayOf(60.0, 1.5, 5.0, 3.5),
        "olives" to doubleArrayOf(115.0, 0.8, 6.0, 11.0),
        "margherita pizza" to doubleArrayOf(266.0, 11.0, 33.0, 10.0),
        "porridge" to doubleArrayOf(95.0, 3.0, 15.0, 2.5),
        "banana" to doubleArrayOf(89.0, 1.1, 23.0, 0.3),
        "apple" to doubleArrayOf(52.0, 0.3, 14.0, 0.2),
        "scrambled eggs" to doubleArrayOf(148.0, 10.0, 1.6, 11.0),
        "avocado toast" to doubleArrayOf(223.0, 6.0, 24.0, 12.0),
        "beef burger" to doubleArrayOf(244.0, 15.0, 20.0, 12.0),
        "fries" to doubleArrayOf(312.0, 3.4, 41.0, 15.0),
        "salmon" to doubleArrayOf(208.0, 20.0, 0.0, 13.0),
        "greek yogurt" to doubleArrayOf(59.0, 10.0, 3.6, 0.4),
    )

    private val ALIASES = mapOf(
        "chicken breast" to "grilled chicken breast",
        "chicken thigh" to "grilled chicken thigh",
        "rice" to "white rice",
        "curry" to "chicken curry",
        "pizza" to "margherita pizza",
        "eggs" to "scrambled eggs",
        "burger" to "beef burger",
        "chips" to "fries",
        "oatmeal" to "porridge",
        "yogurt" to "greek yogurt",
        "salad" to "mixed salad",
    )

    private fun canonical(food: String): String? {
        val q = food.trim().lowercase()
        if (q in TABLE) return q
        ALIASES[q]?.let { return it }
        TABLE.keys.firstOrNull { q in it || it in q }?.let { return it }
        return ALIASES.entries.firstOrNull { (alias, _) -> alias in q || q in alias }?.value
    }

    fun lookup(food: String, grams: Int): Row {
        val key = canonical(food)
            ?: return Row(
                food = food, matched = false, grams = grams,
                kcal = (150.0 * grams / 100.0).roundToInt(),
                protein = 0.0, carbs = 0.0, fat = 0.0,
            )
        val (kcal, p, c, f) = TABLE.getValue(key).let {
            listOf(it[0], it[1], it[2], it[3])
        }
        val factor = grams / 100.0
        return Row(
            food = key, matched = true, grams = grams,
            kcal = (kcal * factor).roundToInt(),
            protein = round1(p * factor),
            carbs = round1(c * factor),
            fat = round1(f * factor),
        )
    }

    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
}
