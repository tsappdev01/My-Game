package com.mathsquest.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mathsquest.core.DailyChallenge
import com.mathsquest.core.Difficulty
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * End-to-end tour of the app with the demo family. Walks every screen as Musfira and the parent,
 * answering the Daily Challenge for real, and saves a screenshot of each step to files/screens.
 * CI pulls the screenshots off the emulator and publishes them.
 */
@RunWith(AndroidJUnit4::class)
class AppTourTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.filesDir, "screens")
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var count = 0

    @Before
    fun setUp() {
        context.deleteDatabase("maths-quest.db")
        dir.deleteRecursively()
        dir.mkdirs()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = scenario.close()

    @Test
    fun tourOfTheApp() {
        // Start: poster, then first-run setup with the demo family.
        Thread.sleep(1_500)
        shot("splash")
        compose.onAllNodesWithContentDescription("Get Started", substring = true).onFirst().performClick()
        waitFor("Just looking around?")
        shot("parent-setup")
        click("Load demo family")
        waitFor("Who's playing today?")
        shot("who-is-playing")

        // Musfira's home and the quest setup screens.
        click("Musfira")
        waitFor("Play Maths")
        shot("home")
        click("Play Maths")
        waitFor("Select Grade")
        shot("select-grade")
        click("Grade 5")
        waitFor("Choose a Topic")
        shot("choose-topic")
        click("Multiplication")
        waitFor("Choose Your Challenge")
        shot("choose-difficulty")
        repeat(3) { back() }
        waitFor("Play Maths")

        // Daily Challenge: questions are deterministic for the day, so the test knows the answers.
        click("Daily Challenge")
        waitFor("Start Challenge")
        shot("daily-challenge")
        click("Start Challenge")
        val today = LocalDate.now().toEpochDay()
        for (i in 0 until DailyChallenge.difficulties.size) {
            val q = DailyChallenge.question(today, 5, i)
            waitForTag("key-check")
            when (i) {
                0 -> {
                    pause(q.difficulty)
                    type(q.answer.toString())
                    shot("question")
                    tap("key-check")
                    waitFor("Correct!")
                    shot("correct")
                    closeLevelUp()
                }
                1 -> {
                    val wrong = (q.answer + 1).toString()
                    type(wrong)
                    tap("key-check")
                    waitFor("Almost!")
                    shot("almost-hint")
                    click("Try Again")
                    waitForTag("key-check")
                    type(wrong)
                    tap("key-check")
                    waitFor("The answer is", substring = true)
                    shot("worked-answer")
                }
                else -> {
                    pause(q.difficulty)
                    type(q.answer.toString())
                    tap("key-check")
                    waitFor("Correct!")
                    closeLevelUp()
                }
            }
            click(if (i == DailyChallenge.difficulties.size - 1) "See results" else "Next Question")
        }
        waitFor("Challenge complete!")
        shot("challenge-complete")
        click("Back home")
        waitFor("Play Maths")
        shot("home-after-challenge")
        click("Daily Challenge")
        waitFor("Leo is resting", substring = true)
        shot("daily-done")
        back()

        // Progress and rewards.
        click("My Progress")
        waitFor("Badges")
        shot("my-progress")
        back()
        click("Rewards")
        waitFor("Reward Shop")
        shot("reward-shop")
        click("Redeem")
        waitFor("My requests")
        shot("reward-requested")
        back()

        // Parent side.
        click("Switch")
        waitFor("Parent area")
        click("Parent area")
        waitFor("Enter your 4-digit PIN")
        click("1"); click("2")
        shot("parent-pin")
        click("3"); click("4")
        waitFor("Parent Dashboard")
        shot("parent-dashboard")
        click("Approve")
        Thread.sleep(500)
        scrollTo("Accuracy by topic")
        shot("parent-topics")
        scrollTo("Coin activity")
        shot("parent-coin-activity")
    }

    // ---- helpers ----------------------------------------------------------------------------

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        count++
        File(dir, "%02d-%s.png".format(count, name)).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private var levelUpShot = false

    /** Musfira levels up during the Daily Challenge: capture the party once, then close it. */
    private fun closeLevelUp() {
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Level up!").fetchSemanticsNodes().isEmpty()) return
        if (!levelUpShot) { shot("level-up"); levelUpShot = true }
        click("Keep going!")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Level up!").fetchSemanticsNodes().isEmpty() }
    }

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(15_000) {
            runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode() }.isSuccess
        }
    }

    private fun click(text: String) {
        waitFor(text)
        val node = compose.onAllNodesWithText(text).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun scrollTo(text: String) {
        waitFor(text)
        compose.onAllNodesWithText(text).onFirst().performScrollTo()
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun type(number: String) = number.forEach { tap("key-$it") }

    private fun back() {
        compose.onAllNodesWithContentDescription("Back").onFirst().performClick()
        compose.waitForIdle()
    }

    /** Waits long enough for the answer to count (fast answers earn no coins). */
    private fun pause(d: Difficulty) = Thread.sleep(d.minAnswerMillis + 300)
}
