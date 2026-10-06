package com.mathsquest.app.data.repo

import androidx.room.withTransaction
import com.mathsquest.app.data.local.AnswerAttemptEntity
import com.mathsquest.app.data.local.ChildEntity
import com.mathsquest.app.data.local.CoinTransactionEntity
import com.mathsquest.app.data.local.MathsQuestDatabase
import com.mathsquest.app.data.local.ParentSettingsEntity
import com.mathsquest.app.data.local.RequestStatus
import com.mathsquest.app.data.local.RewardRequestEntity
import com.mathsquest.app.data.local.TxType
import com.mathsquest.core.Operation
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A demo family for trying the app: Musfira (Grade 5) and Musab (Grade 3), each with a week of
 * practice, coins, a streak and reward history. Mirrors the clickable prototype's sample data.
 */
@Singleton
class DemoData @Inject constructor(
    private val db: MathsQuestDatabase,
    private val repo: MathsQuestRepository,
    private val clock: Clock,
) {
    private data class Spec(
        val name: String,
        val grade: Int,
        val colour: Long,
        val cap: Int,
        val xp: Int,
        val streak: Int,
        val perfect: Boolean,
        /** Operation -> (answered, right first time) this week. */
        val topics: Map<Operation, Pair<Int, Int>>,
        val studyMinutes: Int,
        /** Coins earned before the last few days, shown as one ledger line. */
        val earlierCoins: Int,
        /** (reference, coins, days ago) for recent earnings. */
        val recent: List<Triple<String, Int, Long>>,
        /** (reward title, days ago) approved and redeemed this month. */
        val redeemed: List<Pair<String, Long>>,
        /** Reward titles waiting for a parent. */
        val pending: List<String>,
    )

    private val family = listOf(
        Spec(
            name = "Musfira", grade = 5, colour = 0xFFFFE08AL, cap = 300, xp = 3_865, streak = 6, perfect = false,
            topics = mapOf(Operation.ADD to (30 to 28), Operation.SUB to (20 to 17), Operation.MUL to (22 to 18), Operation.DIV to (12 to 6)),
            studyMinutes = 95,
            earlierCoins = 268,
            recent = listOf(
                Triple("× Grade 5 · Moderate", 2, 0L),
                Triple("× Grade 5 · Moderate", 2, 0L),
                Triple("÷ Grade 5 · Tough", 3, 1L),
                Triple("+ Grade 5 · Moderate", 2, 1L),
                Triple("− Grade 5 · Tough", 3, 2L),
            ),
            redeemed = listOf("Choose Friday dinner" to 3L, "Ice cream outing" to 5L),
            pending = emptyList(),
        ),
        Spec(
            name = "Musab", grade = 3, colour = 0xFFBFDBFFL, cap = 200, xp = 1_240, streak = 2, perfect = true,
            topics = mapOf(Operation.ADD to (18 to 17), Operation.SUB to (12 to 9), Operation.MUL to (8 to 7), Operation.DIV to (3 to 2)),
            studyMinutes = 48,
            earlierCoins = 87,
            recent = listOf(
                Triple("+ Grade 3 · Easy", 1, 0L),
                Triple("− Grade 3 · Moderate", 2, 0L),
                Triple("+ Grade 3 · Easy", 1, 1L),
                Triple("× Grade 3 · Easy", 1, 2L),
            ),
            redeemed = emptyList(),
            pending = listOf("Extra 30 min screen time"),
        ),
    )

    /** First run: sets the parent PIN to 1234 and adds the demo family. */
    suspend fun setUpDemoFamily() {
        db.withTransaction {
            if (db.parentDao().get() == null) {
                val salt = "demo-family-salt"
                db.parentDao().upsert(ParentSettingsEntity(pinHash = repo.hashPin(salt, DEMO_PIN), pinSalt = salt, consentAt = clock.millis()))
            }
            addDemoFamily()
        }
    }

    /** Adds Musfira and Musab unless children with those names already exist. */
    suspend fun addDemoFamily() = db.withTransaction {
        repo.ensureRewards()
        val now = clock.millis()
        val today = LocalDate.now(clock)
        val monthStart = repo.monthStart()
        fun daysAgo(d: Long, hour: Int = 17): Long =
            maxOf(monthStart, today.minusDays(d).atTime(hour, 0).atZone(clock.zone).toInstant().toEpochMilli().coerceAtMost(now))

        for (spec in family) {
            if (db.childDao().countNamed(spec.name) > 0) continue
            val childId = db.childDao().insert(
                ChildEntity(
                    name = spec.name, grade = spec.grade, avatarColor = spec.colour, monthlyCapCoins = spec.cap,
                    xp = spec.xp, streak = spec.streak, lastPracticeDay = today.minusDays(1).toEpochDay(),
                    hadPerfectRound = spec.perfect, createdAt = now - 30L * 86_400_000,
                ),
            )

            // A week of first-try answers spread over the last 6 days.
            val totalAnswers = spec.topics.values.sumOf { it.first }
            val perAnswerMillis = spec.studyMinutes * 60_000L / totalAnswers
            var n = 0
            val attempts = spec.topics.flatMap { (op, counts) ->
                val (answered, right) = counts
                List(answered) { i ->
                    val day = (n++ % 6).toLong() + 1
                    AnswerAttemptEntity(
                        childId = childId,
                        questionId = "demo-${spec.name}-${op.name}-$i",
                        operation = op.name,
                        grade = spec.grade,
                        difficulty = 1 + i % 3,
                        attempt = 1,
                        correct = i < right,
                        elapsedMillis = perAnswerMillis,
                        createdDate = daysAgo(day, 16),
                    )
                }
            }
            db.attemptDao().insertAll(attempts)

            // Ledger: earlier earnings, recent earnings, redeemed rewards.
            db.coinDao().insert(
                CoinTransactionEntity(
                    childId = childId, questionId = null, difficulty = null, coins = spec.earlierCoins,
                    transactionType = TxType.EARN, reference = "Practice earlier this month", createdDate = monthStart,
                ),
            )
            spec.recent.forEachIndexed { i, (ref, coins, ago) ->
                db.coinDao().insert(
                    CoinTransactionEntity(
                        childId = childId, questionId = "demo-recent-$i", difficulty = coins, coins = coins,
                        transactionType = TxType.EARN, reference = ref, createdDate = daysAgo(ago, 15 + i % 3),
                    ),
                )
            }
            spec.redeemed.forEach { (title, ago) ->
                val reward = db.rewardDao().byTitle(title) ?: return@forEach
                val at = daysAgo(ago, 19)
                db.requestDao().insert(
                    RewardRequestEntity(
                        childId = childId, rewardId = reward.id, title = reward.title, cost = reward.cost,
                        status = RequestStatus.APPROVED, createdDate = at - 3_600_000, decidedDate = at,
                    ),
                )
                db.coinDao().insert(
                    CoinTransactionEntity(
                        childId = childId, questionId = null, difficulty = null, coins = -reward.cost,
                        transactionType = TxType.REDEEM, reference = "Reward: ${reward.title}", createdDate = at,
                    ),
                )
            }
            spec.pending.forEach { title ->
                val reward = db.rewardDao().byTitle(title) ?: return@forEach
                db.requestDao().insert(
                    RewardRequestEntity(
                        childId = childId, rewardId = reward.id, title = reward.title, cost = reward.cost,
                        status = RequestStatus.PENDING, createdDate = now - 2 * 3_600_000,
                    ),
                )
            }
        }
    }

    companion object {
        const val DEMO_PIN = "1234"
        val NAMES = listOf("Musfira", "Musab")
    }
}
