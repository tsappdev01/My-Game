package com.mathsquest.app.data.repo

import androidx.room.withTransaction
import com.mathsquest.app.data.local.AnswerAttemptEntity
import com.mathsquest.app.data.local.ChildEntity
import com.mathsquest.app.data.local.CoinTransactionEntity
import com.mathsquest.app.data.local.MathsQuestDatabase
import com.mathsquest.app.data.local.ParentSettingsEntity
import com.mathsquest.app.data.local.RequestStatus
import com.mathsquest.app.data.local.RewardEntity
import com.mathsquest.app.data.local.RewardRequestEntity
import com.mathsquest.app.data.local.TxType
import com.mathsquest.core.CoinContext
import com.mathsquest.core.CoinOutcome
import com.mathsquest.core.CoinRules
import com.mathsquest.core.Operation
import com.mathsquest.core.Question
import com.mathsquest.core.Streaks
import com.mathsquest.core.TopicStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class WeekSummary(val answered: Int, val correctFirstTry: Int, val studyMillis: Long, val coinsEarned: Int) {
    val accuracyPercent: Int get() = if (answered == 0) 0 else Math.round(correctFirstTry * 100f / answered)
}

data class RoundResult(val streak: Int, val streakHit: Boolean, val bonusCoins: Int)

/**
 * Single source of truth for the app in V1 (offline). Every coin movement goes through the
 * append-only ledger, and every payout re-applies [CoinRules] with the child's live monthly total.
 */
@Singleton
class MathsQuestRepository @Inject constructor(
    private val db: MathsQuestDatabase,
    private val clock: Clock,
) {
    private val childDao = db.childDao()
    private val coinDao = db.coinDao()
    private val attemptDao = db.attemptDao()
    private val rewardDao = db.rewardDao()
    private val requestDao = db.requestDao()
    private val parentDao = db.parentDao()

    // ---- time -------------------------------------------------------------------------------

    fun now(): Long = clock.millis()
    fun today(): Long = LocalDate.now(clock).toEpochDay()
    private fun startOf(date: LocalDate): Long = date.atStartOfDay(clock.zone).toInstant().toEpochMilli()
    fun monthStart(): Long = startOf(LocalDate.now(clock).withDayOfMonth(1))
    /** The last 7 days including today. */
    fun weekStart(): Long = startOf(LocalDate.now(clock).minusDays(6))

    // ---- parent -----------------------------------------------------------------------------

    val isSetUp: Flow<Boolean> = parentDao.observeIsSetUp()

    suspend fun completeSetup(pin: String, childName: String, grade: Int, monthlyCap: Int) {
        val salt = newSalt()
        db.withTransaction {
            parentDao.upsert(ParentSettingsEntity(pinHash = hashPin(salt, pin), pinSalt = salt, consentAt = now()))
            ensureRewards()
            childDao.insert(newChild(childName, grade, monthlyCap, childDao.count()))
        }
    }

    suspend fun ensureRewards() {
        if (rewardDao.count() == 0) rewardDao.insertAll(DEFAULT_REWARDS)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val settings = parentDao.get() ?: return false
        return hashPin(settings.pinSalt, pin) == settings.pinHash
    }

    suspend fun addChild(name: String, grade: Int, monthlyCap: Int): Long =
        childDao.insert(newChild(name, grade, monthlyCap, childDao.count()))

    private fun newChild(name: String, grade: Int, cap: Int, colourIndex: Int) = ChildEntity(
        name = name.trim(),
        grade = grade,
        avatarColor = AVATAR_COLOURS[Math.floorMod(colourIndex, AVATAR_COLOURS.size)],
        monthlyCapCoins = cap,
        createdAt = now(),
    )

    // ---- children ---------------------------------------------------------------------------

    fun observeChildren(): Flow<List<ChildEntity>> = childDao.observeAll()
    fun observeChild(id: Long): Flow<ChildEntity?> = childDao.observe(id)
    suspend fun child(id: Long): ChildEntity? = childDao.get(id)

    /** Balance minus coins on hold for pending reward requests. */
    fun observeAvailable(childId: Long): Flow<Int> =
        combine(coinDao.observeBalance(childId), requestDao.observeHeld(childId)) { balance, held -> balance - held }

    fun observeBalance(childId: Long): Flow<Int> = coinDao.observeBalance(childId)
    fun observeHeld(childId: Long): Flow<Int> = requestDao.observeHeld(childId)
    fun observeEarnedThisMonth(childId: Long): Flow<Int> = coinDao.observeEarnedSince(childId, monthStart())
    suspend fun earnedThisMonth(childId: Long): Int = coinDao.earnedSince(childId, monthStart())

    suspend fun setMonthlyCap(childId: Long, cap: Int) {
        val c = childDao.get(childId) ?: return
        childDao.update(c.copy(monthlyCapCoins = cap.coerceIn(MIN_CAP, MAX_CAP)))
    }

    fun displayedStreak(child: ChildEntity): Int = Streaks.displayed(child.streak, child.lastPracticeDay, today())

    // ---- practice ---------------------------------------------------------------------------

    /**
     * Records one answer. Accuracy stats count first attempts only. Returns the payout for a
     * correct answer, or null for a wrong one.
     */
    suspend fun recordAnswer(
        childId: Long,
        question: Question,
        attempt: Int,
        correct: Boolean,
        elapsedMillis: Long,
        daily: Boolean,
    ): CoinOutcome? = db.withTransaction {
        val child = childDao.get(childId) ?: return@withTransaction null
        attemptDao.insert(
            AnswerAttemptEntity(
                childId = childId,
                questionId = question.id,
                operation = question.operation.name,
                grade = question.grade,
                difficulty = question.difficulty.level,
                attempt = attempt,
                correct = correct,
                elapsedMillis = elapsedMillis,
                createdDate = now(),
            ),
        )
        if (!correct) return@withTransaction null
        val outcome = CoinRules.evaluate(
            CoinContext(
                childGrade = child.grade,
                questionGrade = question.grade,
                difficulty = question.difficulty,
                attempt = attempt,
                elapsedMillis = elapsedMillis,
                earnedThisMonth = coinDao.earnedSince(childId, monthStart()),
                monthlyCap = child.monthlyCapCoins,
            ),
        )
        if (outcome.coins > 0) {
            val label = (if (daily) "Daily · " else "") + "${question.operation.symbol} Grade ${question.grade} · ${question.difficulty.label}" +
                (if (attempt > 1) " (retry)" else "")
            coinDao.insert(
                CoinTransactionEntity(
                    childId = childId,
                    questionId = question.id,
                    difficulty = question.difficulty.level,
                    coins = outcome.coins,
                    transactionType = TxType.EARN,
                    reference = label,
                    createdDate = now(),
                ),
            )
        }
        if (outcome.xp > 0) childDao.update(child.copy(xp = child.xp + outcome.xp))
        outcome
    }

    /** Called when a round ends: extends the streak, pays the 7-day bonus, marks the Daily Challenge done. */
    suspend fun finishRound(childId: Long, perfect: Boolean, daily: Boolean): RoundResult = db.withTransaction {
        val child = childDao.get(childId) ?: return@withTransaction RoundResult(0, false, 0)
        val today = today()
        val newStreak = Streaks.afterPractice(child.streak, child.lastPracticeDay, today)
        val streakHit = child.lastPracticeDay != today && newStreak == CoinRules.STREAK_BONUS_DAYS
        var bonus = 0
        if (streakHit) {
            val room = child.monthlyCapCoins - coinDao.earnedSince(childId, monthStart())
            bonus = CoinRules.STREAK_BONUS_COINS.coerceAtMost(room).coerceAtLeast(0)
            if (bonus > 0) {
                coinDao.insert(
                    CoinTransactionEntity(
                        childId = childId, questionId = null, difficulty = null, coins = bonus,
                        transactionType = TxType.BONUS, reference = "7-day streak bonus", createdDate = now(),
                    ),
                )
            }
        }
        childDao.update(
            child.copy(
                streak = newStreak,
                lastPracticeDay = today,
                dailyDoneDay = if (daily) today else child.dailyDoneDay,
                hadPerfectRound = child.hadPerfectRound || perfect,
            ),
        )
        RoundResult(newStreak, streakHit, bonus)
    }

    // ---- stats ------------------------------------------------------------------------------

    fun observeOperationStats(childId: Long): Flow<Map<Operation, TopicStats>> =
        attemptDao.observeOperationCounts(childId).map { rows ->
            Operation.entries.associateWith { op ->
                rows.firstOrNull { it.operation == op.name }?.let { TopicStats(it.answered, it.correctFirstTry) } ?: TopicStats(0, 0)
            }
        }

    fun observeWeek(childId: Long): Flow<WeekSummary> {
        val since = weekStart()
        return combine(
            attemptDao.observeWeek(childId, since),
            attemptDao.observeStudyMillis(childId, since),
            coinDao.observeEarnedSince(childId, since),
        ) { totals, millis, coins -> WeekSummary(totals.answered, totals.correctFirstTry, millis, coins) }
    }

    fun observeLedger(childId: Long, limit: Int = 8): Flow<List<CoinTransactionEntity>> = coinDao.observeRecent(childId, limit)

    // ---- rewards ----------------------------------------------------------------------------

    fun observeRewards(): Flow<List<RewardEntity>> = rewardDao.observeActive()
    fun observeRequestsFor(childId: Long): Flow<List<RewardRequestEntity>> = requestDao.observeForChild(childId)
    fun observePendingRequests(): Flow<List<RewardRequestEntity>> = requestDao.observePending()

    /** Puts the reward's coins on hold until a parent decides. Returns false if the child can't afford it. */
    suspend fun requestReward(childId: Long, rewardId: Long): Boolean = db.withTransaction {
        val reward = rewardDao.get(rewardId) ?: return@withTransaction false
        val available = coinDao.balance(childId) - requestDao.held(childId)
        if (available < reward.cost) return@withTransaction false
        requestDao.insert(
            RewardRequestEntity(
                childId = childId, rewardId = reward.id, title = reward.title, cost = reward.cost,
                status = RequestStatus.PENDING, createdDate = now(),
            ),
        )
        true
    }

    /** Approve writes a REDEEM row to the ledger; decline releases the hold. */
    suspend fun decide(requestId: Long, approve: Boolean) = db.withTransaction {
        val req = requestDao.get(requestId) ?: return@withTransaction
        if (req.status != RequestStatus.PENDING) return@withTransaction
        requestDao.update(req.copy(status = if (approve) RequestStatus.APPROVED else RequestStatus.DECLINED, decidedDate = now()))
        if (approve) {
            coinDao.insert(
                CoinTransactionEntity(
                    childId = req.childId, questionId = null, difficulty = null, coins = -req.cost,
                    transactionType = TxType.REDEEM, reference = "Reward: ${req.title}", createdDate = now(),
                ),
            )
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    private fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hashPin(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        const val MIN_CAP = 50
        const val MAX_CAP = 2_000
        const val DEFAULT_CAP = 300

        val AVATAR_COLOURS = listOf(0xFFFFE08AL, 0xFFBFDBFFL, 0xFFC9F2D6L, 0xFFFFD1E3L, 0xFFE3D6FFL)

        val DEFAULT_REWARDS = listOf(
            RewardEntity(title = "Extra 30 min screen time", cost = 40, icon = "screen"),
            RewardEntity(title = "Choose Friday dinner", cost = 60, icon = "dinner"),
            RewardEntity(title = "Ice cream outing", cost = 80, icon = "icecream"),
            RewardEntity(title = "Trip to the park with a friend", cost = 120, icon = "park"),
            RewardEntity(title = "A small toy you pick together", cost = 200, icon = "toy"),
        )
    }
}
