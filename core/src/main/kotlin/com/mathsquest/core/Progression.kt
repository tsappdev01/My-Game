package com.mathsquest.core

/** Daily practice streak. Days are epoch days in the device's time zone. */
object Streaks {
    /** Returns the new streak after practising on [today]. */
    fun afterPractice(current: Int, lastPracticeDay: Long?, today: Long): Int = when (lastPracticeDay) {
        today -> current
        today - 1 -> current + 1
        else -> 1
    }

    /** A streak shown on screen drops to 0 once a whole day has been missed. */
    fun displayed(current: Int, lastPracticeDay: Long?, today: Long): Int =
        if (lastPracticeDay != null && lastPracticeDay >= today - 1) current else 0
}

/** The Daily Challenge: 10 mixed questions at the child's grade, the same set for everyone on that grade that day. */
object DailyChallenge {
    val difficulties: List<Difficulty> = listOf(
        Difficulty.EASY, Difficulty.EASY, Difficulty.EASY,
        Difficulty.MODERATE, Difficulty.MODERATE, Difficulty.MODERATE, Difficulty.MODERATE,
        Difficulty.TOUGH, Difficulty.TOUGH, Difficulty.TOUGH,
    )
    val maxCoins: Int = difficulties.sumOf { it.baseCoins }

    fun seed(epochDay: Long, grade: Int): Long = epochDay * 31 + grade * 7

    fun question(epochDay: Long, grade: Int, index: Int): Question =
        QuestionEngine.generate(grade, Topic.MIXED, difficulties[index], index, seed(epochDay, grade))
}

/** Per-operation totals used for accuracy, badges and the parent's strong/weak topics. */
data class TopicStats(val answered: Int, val correctFirstTry: Int) {
    val accuracy: Float get() = if (answered == 0) 0f else correctFirstTry / answered.toFloat()
    val percent: Int get() = Math.round(accuracy * 100)
}

data class Badge(val id: String, val name: String, val glyph: String, val earned: Boolean, val status: String)

object Badges {
    private const val MASTER_MIN_QUESTIONS = 20
    private const val MASTER_ACCURACY = 0.85f

    fun evaluate(topics: Map<Operation, TopicStats>, streak: Int, hadPerfectRound: Boolean): List<Badge> {
        fun master(id: String, name: String, glyph: String, op: Operation): Badge {
            val t = topics[op] ?: TopicStats(0, 0)
            val ok = t.answered >= MASTER_MIN_QUESTIONS && t.accuracy >= MASTER_ACCURACY
            val status = if (t.answered < MASTER_MIN_QUESTIONS) "${MASTER_MIN_QUESTIONS - t.answered} more questions" else "Reach 85% right"
            return Badge(id, name, glyph, ok, if (ok) "Earned!" else status)
        }
        val total = topics.values.sumOf { it.answered }
        val daysLeft = CoinRules.STREAK_BONUS_DAYS - streak
        return listOf(
            master("add_master", "Addition Master", "+", Operation.ADD),
            master("mul_master", "Multiplication Master", "×", Operation.MUL),
            master("div_champion", "Division Champion", "÷", Operation.DIV),
            Badge("streak7", "7-Day Streak", "7", streak >= 7, if (streak >= 7) "Earned!" else "$daysLeft more ${if (daysLeft == 1) "day" else "days"}"),
            Badge("q100", "100 Questions", "★", total >= 100, if (total >= 100) "Earned!" else "${100 - total} to go"),
            Badge("perfect", "Perfect Round", "5", hadPerfectRound, if (hadPerfectRound) "Earned!" else "Get 5 of 5 right"),
        )
    }
}
