package com.judgemycal.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.judgemycal.app.data.MealRepository
import com.judgemycal.app.ui.JudgeMyCalRoot
import com.judgemycal.app.ui.JudgeMyCalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The safety floor's break-character path, as an instrumented test so it can
 * never regress silently (spec §6): a distress signal must surface the support
 * message and must NOT reach the agent — same structural guarantee as the
 * backend's before-model callback, verified at the UI level.
 */
@RunWith(AndroidJUnit4::class)
class SafetyFloorTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var fake: FakeAgentService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fake = FakeAgentService()
        AppGraph.mealRepository = MealRepository(context)
        AppGraph.agentService = fake
        compose.setContent {
            JudgeMyCalTheme { JudgeMyCalRoot() }
        }
    }

    @Test
    fun distressNote_breaksCharacter_andNeverReachesTheAgent() {
        compose.onNodeWithTag("nav_capture").performClick()
        compose.onNodeWithTag("note_input").performTextInput(
            "I feel like I should just stop eating",
        )
        compose.onNodeWithTag("demo_meal").performClick()

        // The support message owns the screen…
        compose.onNodeWithTag("support_message").assertIsDisplayed()
        // …and the agent was never consulted. Structural, not stylistic.
        assertEquals(0, fake.estimateCalls.get())

        compose.onNodeWithTag("support_dismiss").performClick()
    }

    @Test
    fun distressBasketItem_alsoBreaksCharacter() {
        compose.onNodeWithTag("nav_shopping").performClick()
        compose.onNodeWithTag("basket_input").performTextInput("I hate my body")
        compose.onNodeWithTag("basket_add").performClick()

        compose.onNodeWithTag("support_message").assertIsDisplayed()
        assertEquals(0, fake.swapCalls.get())
    }
}
