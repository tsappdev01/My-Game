package com.mathsquest.core

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Generates arithmetic questions deterministically per grade, topic and difficulty.
 * Every question carries a worked explanation and a hint. Grades above 7 reuse the
 * Grade 7 ranges until V2 adds negatives, fractions and algebra.
 */
object QuestionEngine {

    // Max operand for + and −, per grade, for Easy / Moderate / Tough.
    private val ADD_MAX = mapOf(
        3 to longArrayOf(20, 100, 500),
        4 to longArrayOf(100, 1_000, 5_000),
        5 to longArrayOf(1_000, 5_000, 20_000),
        6 to longArrayOf(5_000, 20_000, 100_000),
        7 to longArrayOf(10_000, 100_000, 1_000_000),
    )

    // Max factors (a, b) for ×, per grade and difficulty.
    private val MUL_MAX = mapOf(
        3 to arrayOf(5L to 5L, 10L to 10L, 12L to 10L),
        4 to arrayOf(10L to 10L, 12L to 12L, 99L to 9L),
        5 to arrayOf(12L to 12L, 99L to 9L, 999L to 99L),
        6 to arrayOf(99L to 9L, 99L to 99L, 999L to 99L),
        7 to arrayOf(99L to 9L, 99L to 99L, 999L to 99L),
    )

    // Max (divisor, quotient) for ÷. Dividends are built as divisor × quotient, so division is always exact.
    private val DIV_MAX = mapOf(
        3 to arrayOf(5L to 5L, 10L to 10L, 10L to 12L),
        4 to arrayOf(10L to 10L, 12L to 12L, 9L to 99L),
        5 to arrayOf(12L to 12L, 9L to 99L, 12L to 99L),
        6 to arrayOf(9L to 99L, 12L to 99L, 25L to 99L),
        7 to arrayOf(12L to 99L, 25L to 99L, 50L to 999L),
    )

    fun generate(grade: Int, topic: Topic, difficulty: Difficulty, index: Int, seed: Long): Question {
        require(grade in Grades.MIN..Grades.MAX) { "Grade must be ${Grades.MIN}-${Grades.MAX}" }
        val mix = seed * 7_919L + index * 104_729L + grade * 31L + difficulty.level * 17L + topic.label.length
        val rng = Mulberry32(mix.toInt())
        val op = topic.operation ?: Operation.entries[(rng.next() * 4).toInt()]
        val g = grade.coerceIn(3, 7)
        val d = difficulty.level - 1

        val a: Long
        val b: Long
        val answer: Long
        when (op) {
            Operation.ADD, Operation.SUB -> {
                val m = ADD_MAX.getValue(g)[d]
                val lo = max(2L, m / 10)
                var x = rng.nextInt(lo, m)
                var y = rng.nextInt(lo, m)
                if (op == Operation.SUB) {
                    if (x < y) { val t = x; x = y; y = t }
                    if (x == y) x += lo
                    answer = x - y
                } else {
                    answer = x + y
                }
                a = x; b = y
            }
            Operation.MUL -> {
                val (ma, mb) = MUL_MAX.getValue(g)[d]
                val loA = if (ma > 99) 100L else if (ma > 12) 11L else 2L
                val loB = if (mb > 12) 11L else 2L
                a = rng.nextInt(loA, ma)
                b = rng.nextInt(loB, mb)
                answer = a * b
            }
            Operation.DIV -> {
                val (md, mq) = DIV_MAX.getValue(g)[d]
                b = rng.nextInt(2, md)
                answer = rng.nextInt(if (mq > 12) 11L else 2L, mq)
                a = b * answer
            }
        }
        val ex = explain(op, a, b, answer)
        return Question(
            id = "$seed-$index",
            grade = grade,
            topic = topic,
            operation = op,
            difficulty = difficulty,
            a = a,
            b = b,
            answer = answer,
            steps = ex.first,
            hint = ex.second,
        )
    }

    fun format(n: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

    /** Replaces the result after the last "=" with "?" so the hint never gives the answer away. */
    private fun mask(steps: List<String>): List<String> =
        steps.dropLast(1) + steps.last().replace(Regex("= [^=]*$"), "= ?")

    /** Returns (worked steps, hint). */
    internal fun explain(op: Operation, a: Long, b: Long, ans: Long): Pair<List<String>, List<String>> {
        val f = ::format
        return when (op) {
            Operation.ADD -> {
                val big = max(a, b)
                val small = min(a, b)
                val s = when {
                    small < 10 -> listOf("Start at ${f(big)} and count on $small.", "${f(big)} + $small = ${f(ans)}")
                    small % 10 != 0L -> {
                        val ro = small - small % 10
                        val on = small % 10
                        listOf(
                            "Split ${f(small)} into ${f(ro)} and $on.",
                            "${f(big)} + ${f(ro)} = ${f(big + ro)}",
                            "${f(big + ro)} + $on = ${f(ans)}",
                        )
                    }
                    else -> listOf("${f(small)} is a round number, so add it in one jump.", "${f(big)} + ${f(small)} = ${f(ans)}")
                }
                s to mask(s)
            }
            Operation.SUB -> {
                val s = when {
                    b < 10 -> listOf("Count back $b from ${f(a)}.", "${f(a)} − $b = ${f(ans)}")
                    b % 10 != 0L -> {
                        val ro = b - b % 10
                        val on = b % 10
                        listOf(
                            "Split ${f(b)} into ${f(ro)} and $on.",
                            "${f(a)} − ${f(ro)} = ${f(a - ro)}",
                            "${f(a - ro)} − $on = ${f(ans)}",
                        )
                    }
                    else -> listOf("${f(b)} is a round number, so take it away in one jump.", "${f(a)} − ${f(b)} = ${f(ans)}")
                }
                (s + "Check: ${f(ans)} + ${f(b)} = ${f(a)}") to mask(s)
            }
            Operation.MUL -> {
                val s = when {
                    a in 6L..9L -> listOf(
                        "$a × $b means $a groups of $b.",
                        "5 × $b = ${5 * b}",
                        "${a - 5} × $b = ${(a - 5) * b}",
                        "${5 * b} + ${(a - 5) * b} = $ans",
                    )
                    a < 10 -> listOf(
                        "$a × $b means $a groups of $b.",
                        List(a.toInt()) { b.toString() }.joinToString(" + ") + " = $ans",
                    )
                    else -> {
                        val parts = placeValueParts(b)
                        if (parts.size == 1) {
                            listOf("Multiply ${f(a)} by ${f(b)} one digit at a time.", "${f(a)} × ${f(b)} = ${f(ans)}")
                        } else {
                            parts.map { p -> "${f(a)} × ${f(p)} = ${f(a * p)}" } +
                                (parts.joinToString(" + ") { p -> f(a * p) } + " = ${f(ans)}")
                        }
                    }
                }
                s to mask(s)
            }
            Operation.DIV -> {
                val q = ans
                if (q < 10) {
                    val jumps = (1..q).joinToString(", ") { f(b * it) }
                    listOf(
                        "Think: $b × ? = ${f(a)}",
                        "Count in ${b}s: $jumps. That is $q jumps.",
                        "So ${f(a)} ÷ $b = $q",
                    ) to listOf("Think: $b × ? = ${f(a)}", "Count in ${b}s until you reach ${f(a)}.")
                } else {
                    val t = q - q % 10
                    val o = q % 10
                    val s = mutableListOf(
                        "Think: $b × ? = ${f(a)}",
                        "$b × ${f(t)} = ${f(b * t)}. That leaves ${f(a)} − ${f(b * t)} = ${f(a - b * t)}.",
                    )
                    if (o != 0L) s += "$b × $o = ${f(a - b * t)}"
                    s += "${f(t)}${if (o != 0L) " + $o" else ""} = ${f(q)}, so ${f(a)} ÷ $b = ${f(q)}"
                    s to listOf(s[0], "$b × ${f(t)} = ${f(b * t)}. How many more ${b}s make ${f(a)}?")
                }
            }
        }
    }

    /** 347 -> [300, 40, 7], skipping zero digits. */
    private fun placeValueParts(n: Long): List<Long> {
        val digits = n.toString()
        return digits.mapIndexedNotNull { i, c ->
            val place = Math.pow(10.0, (digits.length - 1 - i).toDouble()).toLong()
            val v = (c - '0') * place
            if (v != 0L) v else null
        }
    }
}
