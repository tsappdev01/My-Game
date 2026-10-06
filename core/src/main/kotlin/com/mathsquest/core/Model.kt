package com.mathsquest.core

/** The four arithmetic operations covered in V1. */
enum class Operation(val symbol: String, val label: String) {
    ADD("+", "Addition"),
    SUB("−", "Subtraction"),
    MUL("×", "Multiplication"),
    DIV("÷", "Division"),
}

/** What the child picks on the topic screen. MIXED draws a random operation per question. */
enum class Topic(val label: String, val operation: Operation?) {
    ADD("Addition", Operation.ADD),
    SUB("Subtraction", Operation.SUB),
    MUL("Multiplication", Operation.MUL),
    DIV("Division", Operation.DIV),
    MIXED("Mixed Challenge", null),
}

/**
 * Difficulty sets the number ranges, the base coin payout and the minimum plausible answer time.
 * Answers faster than [minAnswerMillis] earn no coins (anti-guessing).
 */
enum class Difficulty(val level: Int, val label: String, val baseCoins: Int, val minAnswerMillis: Long) {
    EASY(1, "Easy", 1, 1_500),
    MODERATE(2, "Moderate", 2, 2_500),
    TOUGH(3, "Tough", 3, 4_000);

    companion object {
        fun fromLevel(level: Int): Difficulty = entries.first { it.level == level }
    }
}

object Grades {
    const val MIN = 3
    const val MAX = 9
    val all: List<Int> = (MIN..MAX).toList()
}

/** A generated question. [a] and [b] are the operands; [answer] is the exact result (division is always exact). */
data class Question(
    val id: String,
    val grade: Int,
    val topic: Topic,
    val operation: Operation,
    val difficulty: Difficulty,
    val a: Long,
    val b: Long,
    val answer: Long,
    /** Full worked explanation, shown after a second wrong answer. */
    val steps: List<String>,
    /** Hint that stops just before the answer, shown after a first wrong answer. */
    val hint: List<String>,
    /** Name of the method the clue teaches, e.g. "Times 9 trick" or "Count the tally marks". */
    val technique: String = "",
    /** Tally-mark picture for small numbers; null when the numbers are too big to draw. */
    val tally: Tally? = null,
)

/**
 * A tally-mark picture. Each entry in [groups] is one bundle of marks (drawn in fives).
 * [crossedOut] marks at the end of the first bundle are crossed out (subtraction).
 * [joiner] is drawn between bundles ("+" for addition; empty for groups of equal size).
 */
data class Tally(val groups: List<Int>, val crossedOut: Int = 0, val joiner: String = "") {
    val total: Int get() = groups.sum()
}
