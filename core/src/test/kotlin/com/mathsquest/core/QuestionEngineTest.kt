package com.mathsquest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionEngineTest {

    @Test
    fun sameInputsGiveSameQuestion() {
        val q1 = QuestionEngine.generate(5, Topic.MUL, Difficulty.TOUGH, 3, 4242)
        val q2 = QuestionEngine.generate(5, Topic.MUL, Difficulty.TOUGH, 3, 4242)
        assertEquals(q1, q2)
    }

    @Test
    fun answersAreCorrectForEveryGradeTopicAndDifficulty() {
        for (grade in Grades.all) for (topic in Topic.entries) for (d in Difficulty.entries) for (i in 0 until 50) {
            val q = QuestionEngine.generate(grade, topic, d, i, 1000L + i)
            val expected = when (q.operation) {
                Operation.ADD -> q.a + q.b
                Operation.SUB -> q.a - q.b
                Operation.MUL -> q.a * q.b
                Operation.DIV -> {
                    assertEquals("division must be exact: $q", 0L, q.a % q.b)
                    q.a / q.b
                }
            }
            assertEquals(q.toString(), expected, q.answer)
            assertTrue("no negative or zero answers: $q", q.answer > 0)
            if (topic.operation != null) assertEquals(topic.operation, q.operation)
            assertTrue(q.steps.isNotEmpty())
            assertTrue(q.hint.isNotEmpty())
        }
    }

    @Test
    fun finalStepShowsAnswerButHintDoesNot() {
        for (topic in listOf(Topic.ADD, Topic.SUB, Topic.MUL)) for (i in 0 until 30) {
            val q = QuestionEngine.generate(5, topic, Difficulty.MODERATE, i, 77)
            val answer = QuestionEngine.format(q.answer)
            val workedResult = if (q.operation == Operation.SUB) q.steps[q.steps.size - 2] else q.steps.last()
            assertTrue("worked steps end with the answer: ${q.steps}", workedResult.endsWith("= $answer"))
            assertTrue("hint hides the answer: ${q.hint}", q.hint.last().endsWith("= ?"))
        }
    }

    @Test
    fun harderDifficultiesUseBiggerNumbers() {
        fun avg(d: Difficulty) = (0 until 200).map { QuestionEngine.generate(4, Topic.ADD, d, it, 9).answer }.average()
        assertTrue(avg(Difficulty.EASY) < avg(Difficulty.MODERATE))
        assertTrue(avg(Difficulty.MODERATE) < avg(Difficulty.TOUGH))
    }

    @Test
    fun dailyChallengeIsStableForTheDay() {
        val day = 20_000L
        val a = (0 until 10).map { DailyChallenge.question(day, 5, it) }
        val b = (0 until 10).map { DailyChallenge.question(day, 5, it) }
        assertEquals(a, b)
        assertEquals(20, DailyChallenge.maxCoins)
        assertFalse(a == (0 until 10).map { DailyChallenge.question(day + 1, 5, it) })
    }

    @Test
    fun smallNumbersGetTallyMarks() {
        val add = QuestionEngine.method(Operation.ADD, 7, 5, 12)
        assertEquals("Count the tally marks", add.name)
        assertEquals(Tally(listOf(7, 5), joiner = "+"), add.tally)
        val sub = QuestionEngine.method(Operation.SUB, 15, 6, 9)
        assertEquals(Tally(listOf(15), crossedOut = 6), sub.tally)
        val mul = QuestionEngine.method(Operation.MUL, 3, 4, 12)
        assertEquals(Tally(listOf(4, 4, 4)), mul.tally)
        val div = QuestionEngine.method(Operation.DIV, 12, 3, 4)
        assertEquals(Tally(listOf(3, 3, 3, 3)), div.tally)
        assertTrue(div.hint!!.none { it.contains("= 4") })
    }

    @Test
    fun bigNumbersGetMentalMathsTricks() {
        assertEquals("Times 9 trick", QuestionEngine.method(Operation.MUL, 47, 9, 423).name)
        assertEquals("Times 11 trick", QuestionEngine.method(Operation.MUL, 11, 34, 374).name)
        assertEquals("Times 5 trick", QuestionEngine.method(Operation.MUL, 68, 5, 340).name)
        assertEquals("Round and adjust", QuestionEngine.method(Operation.MUL, 23, 19, 437).name)
        assertEquals("Round and adjust", QuestionEngine.method(Operation.ADD, 298, 47, 345).name)
        assertEquals("Round and adjust", QuestionEngine.method(Operation.SUB, 512, 289, 223).name)
        assertEquals("Count up", QuestionEngine.method(Operation.SUB, 103, 86, 17).name)
        assertEquals("Halve twice", QuestionEngine.method(Operation.DIV, 96, 4, 24).name)
        assertEquals("Double, then divide by 10", QuestionEngine.method(Operation.DIV, 85, 5, 17).name)
    }

    @Test
    fun everyMethodEndsWithTheRightAnswer() {
        for (grade in Grades.all) for (topic in Topic.entries) for (d in Difficulty.entries) for (i in 0 until 40) {
            val q = QuestionEngine.generate(grade, topic, d, i, 4321L + i)
            val answer = QuestionEngine.format(q.answer)
            val last = if (q.operation == Operation.SUB || q.operation == Operation.DIV) q.steps[q.steps.size - 2] else q.steps.last()
            assertTrue("${q.technique}: $last should end with $answer", last.endsWith(answer))
            assertTrue("${q.technique}: hint must not end with the answer: ${q.hint}", !q.hint.last().endsWith("= $answer"))
            assertTrue(q.technique.isNotBlank())
        }
    }
}
