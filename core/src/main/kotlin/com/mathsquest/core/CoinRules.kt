package com.mathsquest.core

/** Why a correct answer earned fewer coins than its base value. */
enum class CoinReason {
    NONE,
    /** Question grade is 2+ below the child's grade. */
    PRACTICE,
    /** Answered faster than the difficulty's minimum plausible time. */
    TOO_FAST,
    /** Correct on the second attempt (one coin less). */
    RETRY,
    /** Monthly coin limit reached or nearly reached. */
    CAP,
}

data class CoinContext(
    val childGrade: Int,
    val questionGrade: Int,
    val difficulty: Difficulty,
    /** 1 for the first try, 2 after a hint. */
    val attempt: Int,
    val elapsedMillis: Long,
    val earnedThisMonth: Int,
    val monthlyCap: Int,
)

data class CoinOutcome(val coins: Int, val xp: Int, val reason: CoinReason)

/**
 * Coin and XP payout for a correct answer. The API applies the same rules and is the
 * source of truth; the app uses them to show the child what they earned straight away.
 */
object CoinRules {
    const val XP_PER_LEVEL = 500
    const val STREAK_BONUS_DAYS = 7
    const val STREAK_BONUS_COINS = 5
    const val COIN_VALUE_FILS = 10 // 1 coin = AED 0.10 of reward value

    fun isPracticeGrade(childGrade: Int, questionGrade: Int): Boolean = questionGrade < childGrade - 1

    fun evaluate(ctx: CoinContext): CoinOutcome {
        val base = ctx.difficulty.baseCoins
        if (isPracticeGrade(ctx.childGrade, ctx.questionGrade)) return CoinOutcome(0, 5, CoinReason.PRACTICE)
        if (ctx.elapsedMillis < ctx.difficulty.minAnswerMillis) return CoinOutcome(0, 0, CoinReason.TOO_FAST)
        val coins = if (ctx.attempt > 1) base - 1 else base
        val xp = if (ctx.attempt > 1) 5 * ctx.difficulty.level else 10 * ctx.difficulty.level
        if (coins <= 0) return CoinOutcome(0, xp, CoinReason.RETRY)
        val room = ctx.monthlyCap - ctx.earnedThisMonth
        if (room <= 0) return CoinOutcome(0, xp, CoinReason.CAP)
        if (coins > room) return CoinOutcome(room, xp, CoinReason.CAP)
        return CoinOutcome(coins, xp, if (ctx.attempt > 1) CoinReason.RETRY else CoinReason.NONE)
    }

    /** Coins the child would see on the question chip before answering. */
    fun worth(childGrade: Int, questionGrade: Int, difficulty: Difficulty, attempt: Int, earnedThisMonth: Int, monthlyCap: Int): Int {
        if (isPracticeGrade(childGrade, questionGrade)) return 0
        val room = monthlyCap - earnedThisMonth
        if (room <= 0) return 0
        val base = if (attempt > 1) difficulty.baseCoins - 1 else difficulty.baseCoins
        return base.coerceIn(0, room)
    }

    fun level(xp: Int): Int = xp / XP_PER_LEVEL + 1
    fun levelProgress(xp: Int): Float = (xp % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat()

    /** Formats coins as AED reward value, e.g. 150 -> "AED 15.00". Parents only; children never see money. */
    fun aed(coins: Int): String {
        val fils = coins.toLong() * COIN_VALUE_FILS
        return "AED %d.%02d".format(fils / 100, fils % 100)
    }
}
