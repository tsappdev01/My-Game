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
        val m = method(op, a, b, answer)
        return Question(
            id = "$seed-$index",
            grade = grade,
            topic = topic,
            operation = op,
            difficulty = difficulty,
            a = a,
            b = b,
            answer = answer,
            steps = m.steps + listOfNotNull(m.check),
            hint = m.hint ?: mask(m.steps),
            technique = m.name,
            tally = m.tally,
        )
    }

    fun format(n: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

    /** Replaces the result after the last "=" with "?" so the hint never gives the answer away. */
    private fun mask(steps: List<String>): List<String> =
        steps.dropLast(1) + steps.last().replace(Regex("= [^=]*$"), "= ?")

    /** A worked method: its name, the full steps (last one ends "= answer"), an optional check line and tally picture. */
    internal data class Method(val name: String, val steps: List<String>, val check: String? = null, val tally: Tally? = null, val hint: List<String>? = null)

    /** Tally marks are only drawn when there are few enough to count comfortably. */
    private const val MAX_TALLY = 30

    /** Returns (worked steps, hint) for the method chosen for this question. */
    internal fun explain(op: Operation, a: Long, b: Long, ans: Long): Pair<List<String>, List<String>> =
        method(op, a, b, ans).let { m -> (m.steps + listOfNotNull(m.check)) to (m.hint ?: mask(m.steps)) }

    /**
     * Picks the clearest method for the numbers: tally marks for small ones, then a mental-maths
     * trick when the numbers suit one, otherwise splitting by place value.
     */
    internal fun method(op: Operation, a: Long, b: Long, ans: Long): Method {
        val f = ::format
        return when (op) {
            Operation.ADD -> {
                val big = max(a, b)
                val small = min(a, b)
                val lastDigit = big % 10
                when {
                    ans <= MAX_TALLY -> Method(
                        "Count the tally marks",
                        listOf("Draw $a marks, then $b more.", "Count them all in fives.", "$a + $b = $ans"),
                        tally = Tally(listOf(a.toInt(), b.toInt()), joiner = "+"),
                    )
                    big >= 20 && lastDigit >= 7 -> {
                        val extra = 10 - lastDigit
                        val round = big + extra
                        Method(
                            "Round and adjust",
                            listOf(
                                "Round ${f(big)} up to ${f(round)}. That adds $extra extra.",
                                "${f(round)} + ${f(small)} = ${f(round + small)}",
                                "Take back the $extra: ${f(round + small)} − $extra = ${f(ans)}",
                            ),
                        )
                    }
                    small < 10 -> Method("Count on", listOf("Start at ${f(big)} and count on $small.", "${f(big)} + $small = ${f(ans)}"))
                    small % 10 != 0L -> {
                        val ro = small - small % 10
                        val on = small % 10
                        Method(
                            "Jump in tens, then ones",
                            listOf("Split ${f(small)} into ${f(ro)} and $on.", "${f(big)} + ${f(ro)} = ${f(big + ro)}", "${f(big + ro)} + $on = ${f(ans)}"),
                        )
                    }
                    else -> Method("Add the round number", listOf("${f(small)} is a round number, so add it in one jump.", "${f(big)} + ${f(small)} = ${f(ans)}"))
                }
            }
            Operation.SUB -> {
                val check = "Check: ${f(ans)} + ${f(b)} = ${f(a)}"
                val bLast = b % 10
                when {
                    a <= MAX_TALLY -> Method(
                        "Cross out tally marks",
                        listOf("Draw $a marks.", "Cross out $b of them.", "Count what is left: $a − $b = $ans"),
                        check = check,
                        tally = Tally(listOf(a.toInt()), crossedOut = b.toInt()),
                    )
                    ans <= 30 && b >= 10 && bLast != 0L -> {
                        val nextTen = b + (10 - bLast) % 10
                        val toTen = nextTen - b
                        val rest = a - nextTen
                        Method(
                            "Count up",
                            listOf(
                                "The numbers are close, so count up from ${f(b)} to ${f(a)}.",
                                "${f(b)} to ${f(nextTen)} is $toTen.",
                                "${f(nextTen)} to ${f(a)} is ${f(rest)}.",
                                "$toTen + ${f(rest)} = ${f(ans)}",
                            ),
                            check = check,
                        )
                    }
                    b >= 10 && bLast >= 7 -> {
                        val extra = 10 - bLast
                        val round = b + extra
                        Method(
                            "Round and adjust",
                            listOf(
                                "Take away ${f(round)} instead. That is $extra too many.",
                                "${f(a)} − ${f(round)} = ${f(a - round)}",
                                "Give back the $extra: ${f(a - round)} + $extra = ${f(ans)}",
                            ),
                            check = check,
                        )
                    }
                    b < 10 -> Method("Count back", listOf("Count back $b from ${f(a)}.", "${f(a)} − $b = ${f(ans)}"), check = check)
                    bLast != 0L -> {
                        val ro = b - bLast
                        Method(
                            "Jump back in tens, then ones",
                            listOf("Split ${f(b)} into ${f(ro)} and $bLast.", "${f(a)} − ${f(ro)} = ${f(a - ro)}", "${f(a - ro)} − $bLast = ${f(ans)}"),
                            check = check,
                        )
                    }
                    else -> Method("Take away the round number", listOf("${f(b)} is a round number, so take it away in one jump.", "${f(a)} − ${f(b)} = ${f(ans)}"), check = check)
                }
            }
            Operation.MUL -> multiplyMethod(a, b, ans)
            Operation.DIV -> divideMethod(a, b, ans)
        }
    }

    private fun multiplyMethod(a: Long, b: Long, ans: Long): Method {
        val f = ::format
        // Look for a "special" factor; n is the other one.
        fun pick(test: (Long) -> Boolean): Pair<Long, Long>? = when {
            test(b) -> b to a
            test(a) -> a to b
            else -> null
        }
        if (ans <= MAX_TALLY) {
            val skips = (1..a).joinToString(", ") { f(b * it) }
            return Method(
                "Groups of tally marks",
                listOf("Draw $a groups of $b marks.", "Skip count by ${b}s: $skips.", "$a × $b = $ans"),
                tally = Tally(List(a.toInt()) { b.toInt() }),
            )
        }
        pick { it == 9L }?.let { (_, n) ->
            return Method("Times 9 trick", listOf("Times 9 is times 10, take away one.", "${f(n)} × 10 = ${f(n * 10)}", "${f(n * 10)} − ${f(n)} = ${f(ans)}"))
        }
        pick { it == 11L }?.takeIf { it.second in 10L..99L }?.let { (_, n) ->
            val d1 = n / 10
            val d2 = n % 10
            val mid = d1 + d2
            val carry = if (mid >= 10) " Carry the 1 into the $d1." else ""
            return Method(
                "Times 11 trick",
                listOf(
                    "Pull the digits of ${f(n)} apart: $d1 _ $d2.",
                    "Add them: $d1 + $d2 = $mid.",
                    "Put $mid in the middle.$carry ${f(n)} × 11 = ${f(ans)}",
                ),
            )
        }
        pick { it == 5L }?.let { (_, n) ->
            return Method("Times 5 trick", listOf("Times 5 is half of times 10.", "${f(n)} × 10 = ${f(n * 10)}", "Halve it: ${f(n * 10)} ÷ 2 = ${f(ans)}"))
        }
        pick { it == 25L }?.let { (_, n) ->
            return Method("Times 25 trick", listOf("Times 25 is a quarter of times 100.", "${f(n)} × 100 = ${f(n * 100)}", "Divide by 4: ${f(n * 100)} ÷ 4 = ${f(ans)}"))
        }
        pick { it == 4L }?.let { (_, n) ->
            return Method("Double, then double again", listOf("Times 4 is doubling twice.", "Double ${f(n)}: ${f(n * 2)}", "Double again: ${f(n * 2)} × 2 = ${f(ans)}"))
        }
        pick { it >= 19 && it % 10 == 9L }?.let { (k, n) ->
            val round = k + 1
            return Method(
                "Round and adjust",
                listOf(
                    "${f(k)} is one less than ${f(round)}.",
                    "${f(n)} × ${f(round)} = ${f(n * round)}",
                    "Take away one ${f(n)}: ${f(n * round)} − ${f(n)} = ${f(ans)}",
                ),
            )
        }
        if (a < 10 && a > 5) {
            return Method(
                "Split into 5 and the rest",
                listOf("$a × $b means $a groups of $b.", "5 × $b = ${5 * b}", "${a - 5} × $b = ${(a - 5) * b}", "${5 * b} + ${(a - 5) * b} = $ans"),
            )
        }
        if (a < 10) {
            return Method("Skip count", listOf("$a × $b means $a groups of $b.", "Count in ${b}s: " + (1..a).joinToString(", ") { f(b * it) } + ".", "$a × $b = $ans"))
        }
        val parts = placeValueParts(b)
        if (parts.size == 1) {
            return Method("Multiply, then add the zeros", listOf("Multiply ${f(a)} by ${f(b)} one digit at a time.", "${f(a)} × ${f(b)} = ${f(ans)}"))
        }
        return Method(
            "Split and multiply",
            parts.map { p -> "${f(a)} × ${f(p)} = ${f(a * p)}" } + (parts.joinToString(" + ") { p -> f(a * p) } + " = ${f(ans)}"),
        )
    }

    private fun divideMethod(a: Long, b: Long, q: Long): Method {
        val f = ::format
        val check = "Check: $b × ${f(q)} = ${f(a)}"
        return when {
            a <= MAX_TALLY -> Method(
                "Share the tally marks",
                listOf("Draw $a marks.", "Circle groups of $b.", "Count the groups: $a ÷ $b = $q"),
                check = check,
                tally = Tally(List(q.toInt()) { b.toInt() }),
                hint = listOf("Draw $a marks.", "Circle groups of $b.", "How many groups can you make?"),
            )
            b == 2L -> Method("Halve it", listOf("Dividing by 2 means halving.", "Half of ${f(a)} = ${f(q)}"), check = check)
            b == 4L -> Method("Halve twice", listOf("Dividing by 4 is halving twice.", "Half of ${f(a)} = ${f(a / 2)}", "Half again: ${f(a / 2)} ÷ 2 = ${f(q)}"), check = check)
            b == 8L -> Method(
                "Halve three times",
                listOf("Dividing by 8 is halving three times.", "Half of ${f(a)} = ${f(a / 2)}", "Half again = ${f(a / 4)}", "And again: ${f(a / 4)} ÷ 2 = ${f(q)}"),
                check = check,
            )
            b == 5L -> Method("Double, then divide by 10", listOf("Dividing by 5 is doubling, then dividing by 10.", "${f(a)} × 2 = ${f(a * 2)}", "${f(a * 2)} ÷ 10 = ${f(q)}"), check = check)
            b == 25L -> Method("Times 4, then divide by 100", listOf("Dividing by 25 is times 4, then divide by 100.", "${f(a)} × 4 = ${f(a * 4)}", "${f(a * 4)} ÷ 100 = ${f(q)}"), check = check)
            q < 10 -> {
                val jumps = (1..q).joinToString(", ") { f(b * it) }
                Method(
                    "Skip count",
                    listOf("Think: $b × ? = ${f(a)}", "Count in ${b}s: $jumps. That is $q jumps.", "So ${f(a)} ÷ $b = $q"),
                    check = check,
                    hint = listOf("Think: $b × ? = ${f(a)}", "Count in ${b}s until you reach ${f(a)}."),
                )
            }
            else -> {
                val t = q - q % 10
                val o = q % 10
                val s = mutableListOf(
                    "Think: $b × ? = ${f(a)}",
                    "$b × ${f(t)} = ${f(b * t)}. That leaves ${f(a)} − ${f(b * t)} = ${f(a - b * t)}.",
                )
                if (o != 0L) s += "$b × $o = ${f(a - b * t)}"
                s += "${f(t)}${if (o != 0L) " + $o" else ""} = ${f(q)}, so ${f(a)} ÷ $b = ${f(q)}"
                Method(
                    "Chunking",
                    s,
                    check = check,
                    hint = listOf(s[0], "$b × ${f(t)} = ${f(b * t)}. How many more ${b}s make ${f(a)}?"),
                )
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
