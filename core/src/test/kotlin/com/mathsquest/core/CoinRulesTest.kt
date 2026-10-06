package com.mathsquest.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoinRulesTest {
    private fun ctx(
        childGrade: Int = 5,
        questionGrade: Int = 5,
        difficulty: Difficulty = Difficulty.MODERATE,
        attempt: Int = 1,
        elapsed: Long = 10_000,
        earned: Int = 0,
        cap: Int = 300,
    ) = CoinContext(childGrade, questionGrade, difficulty, attempt, elapsed, earned, cap)

    @Test fun paysBaseCoins() = assertEquals(CoinOutcome(2, 20, CoinReason.NONE), CoinRules.evaluate(ctx()))

    @Test fun oneGradeBelowStillPays() = assertEquals(2, CoinRules.evaluate(ctx(questionGrade = 4)).coins)

    @Test fun twoGradesBelowIsPractice() = assertEquals(CoinOutcome(0, 5, CoinReason.PRACTICE), CoinRules.evaluate(ctx(questionGrade = 3)))

    @Test fun tooFastPaysNothing() = assertEquals(CoinOutcome(0, 0, CoinReason.TOO_FAST), CoinRules.evaluate(ctx(elapsed = 2_000)))

    @Test fun retryPaysOneLess() = assertEquals(CoinOutcome(1, 10, CoinReason.RETRY), CoinRules.evaluate(ctx(attempt = 2)))

    @Test fun easyRetryPaysNothing() = assertEquals(0, CoinRules.evaluate(ctx(difficulty = Difficulty.EASY, attempt = 2)).coins)

    @Test fun capLimitsPayout() {
        assertEquals(CoinOutcome(1, 30, CoinReason.CAP), CoinRules.evaluate(ctx(difficulty = Difficulty.TOUGH, earned = 299)))
        assertEquals(CoinOutcome(0, 30, CoinReason.CAP), CoinRules.evaluate(ctx(difficulty = Difficulty.TOUGH, earned = 300)))
    }

    @Test fun levels() {
        assertEquals(1, CoinRules.level(0))
        assertEquals(8, CoinRules.level(3865))
        assertEquals(0.73f, CoinRules.levelProgress(3865), 0.001f)
    }

    @Test fun aed() = assertEquals("AED 15.00", CoinRules.aed(150))

    @Test fun streaks() {
        assertEquals(7, Streaks.afterPractice(6, 99, 100))
        assertEquals(6, Streaks.afterPractice(6, 100, 100))
        assertEquals(1, Streaks.afterPractice(6, 97, 100))
        assertEquals(0, Streaks.displayed(6, 97, 100))
    }
}
