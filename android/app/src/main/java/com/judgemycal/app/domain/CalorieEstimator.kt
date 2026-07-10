package com.judgemycal.app.domain

import kotlin.math.roundToInt

/**
 * On-device fallback estimator, a faithful port of the backend's `estimator.py`:
 * mock recognition + real reconciliation against [NutritionDb]. Per-item bands
 * widen with uncertainty, and the meal's overall confidence is the LOWEST
 * item's — never an average — because one shaky portion makes the whole total
 * uncertain. Used only when the backend is unreachable (or no backend is
 * configured); results are flagged `fromFallback`.
 */
object CalorieEstimator {

    private val BAND = mapOf(
        Confidence.HIGH to 0.10,
        Confidence.MEDIUM to 0.20,
        Confidence.LOW to 0.35,
    )

    private data class Recognized(val food: String, val grams: Int, val confidence: Confidence)

    // Portion confidence reflects how hard the portion is to judge from a photo
    // (rice/curry are notoriously hard; a countable banana is easy).
    private val SAMPLES = listOf(
        listOf(
            Recognized("grilled chicken thigh", 140, Confidence.HIGH),
            Recognized("white rice", 180, Confidence.LOW),
            Recognized("mixed salad", 90, Confidence.MEDIUM),
        ),
        listOf(
            Recognized("margherita pizza", 220, Confidence.MEDIUM),
            Recognized("olives", 40, Confidence.HIGH),
        ),
        listOf(
            Recognized("porridge", 300, Confidence.HIGH),
            Recognized("banana", 120, Confidence.HIGH),
        ),
        listOf(
            Recognized("chicken curry", 250, Confidence.LOW),
            Recognized("naan", 90, Confidence.MEDIUM),
            Recognized("basmati rice", 150, Confidence.LOW),
        ),
        listOf(
            Recognized("scrambled eggs", 150, Confidence.HIGH),
            Recognized("avocado toast", 130, Confidence.MEDIUM),
        ),
        listOf(
            Recognized("beef burger", 250, Confidence.MEDIUM),
            Recognized("fries", 130, Confidence.LOW),
        ),
    )

    fun estimate(seed: Long = System.currentTimeMillis(), note: String = ""): MealEstimate {
        val sample = SAMPLES[((seed % SAMPLES.size + SAMPLES.size) % SAMPLES.size).toInt()]
        val items = sample.map { rec ->
            val row = NutritionDb.lookup(rec.food, rec.grams)
            // An unmatched food can never be better than LOW confidence.
            val conf = if (row.matched) rec.confidence else Confidence.LOW
            val band = BAND.getValue(conf)
            MealItem(
                name = row.food,
                grams = rec.grams,
                kcal = row.kcal,
                kcalLow = (row.kcal * (1 - band)).roundToInt(),
                kcalHigh = (row.kcal * (1 + band)).roundToInt(),
                protein = row.protein,
                carbs = row.carbs,
                fat = row.fat,
                confidence = conf,
            )
        }
        return fromItems(items, note)
    }

    /** Rebuild the meal after a one-tap portion correction on a single item. */
    fun withPortion(estimate: MealEstimate, itemName: String, grams: Int): MealEstimate {
        val items = estimate.items.map { item ->
            if (item.name != itemName) item else {
                val row = NutritionDb.lookup(item.name, grams)
                // The user just told us the portion — that item is now HIGH confidence.
                val band = BAND.getValue(Confidence.HIGH)
                item.copy(
                    grams = grams,
                    kcal = row.kcal,
                    kcalLow = (row.kcal * (1 - band)).roundToInt(),
                    kcalHigh = (row.kcal * (1 + band)).roundToInt(),
                    protein = row.protein,
                    carbs = row.carbs,
                    fat = row.fat,
                    confidence = Confidence.HIGH,
                )
            }
        }
        return fromItems(items, estimate.note).copy(
            fromFallback = estimate.fromFallback,
            personaReaction = estimate.personaReaction,
        )
    }

    private fun fromItems(items: List<MealItem>, note: String): MealEstimate {
        val overall = items.maxByOrNull { it.confidence.ordinal }?.confidence ?: Confidence.LOW
        val lows = items.filter { it.confidence == Confidence.LOW }
        return MealEstimate(
            items = items,
            totalKcal = items.sumOf { it.kcal },
            totalLow = items.sumOf { it.kcalLow },
            totalHigh = items.sumOf { it.kcalHigh },
            protein = round1(items.sumOf { it.protein }),
            carbs = round1(items.sumOf { it.carbs }),
            fat = round1(items.sumOf { it.fat }),
            overallConfidence = overall,
            lowestConfidenceItem = lows.minByOrNull { it.kcal }?.name,
            note = note,
            fromFallback = true,
        )
    }

    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
}
