package com.mathsquest.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grade: Int,
    val avatarColor: Long,
    /** Parent-set monthly coin limit, enforced on every payout. */
    val monthlyCapCoins: Int = 300,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastPracticeDay: Long? = null,
    val dailyDoneDay: Long? = null,
    val hadPerfectRound: Boolean = false,
    val createdAt: Long,
)

/**
 * Append-only coin ledger. Rows are never updated or deleted; a child's balance is SUM(coins).
 * Mirrors the server's CoinTransaction table.
 */
@Entity(tableName = "coin_transactions", indices = [Index("childId"), Index("createdDate")])
data class CoinTransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val childId: Long,
    val questionId: String?,
    val difficulty: Int?,
    val coins: Int,
    /** EARN, BONUS or REDEEM. */
    val transactionType: String,
    /** Human-readable reference, e.g. "× Grade 5 · Moderate" or "Reward: Ice cream outing". */
    val reference: String,
    val createdDate: Long,
)

@Entity(tableName = "answer_attempts", indices = [Index("childId"), Index("createdDate")])
data class AnswerAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val questionId: String,
    val operation: String,
    val grade: Int,
    val difficulty: Int,
    val attempt: Int,
    val correct: Boolean,
    val elapsedMillis: Long,
    val createdDate: Long,
)

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cost: Int,
    /** Key into the app's reward icon set. */
    val icon: String,
    val active: Boolean = true,
)

@Entity(tableName = "reward_requests", indices = [Index("childId"), Index("status")])
data class RewardRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val rewardId: Long,
    val title: String,
    val cost: Int,
    /** PENDING, APPROVED or DECLINED. Coins for PENDING requests are on hold. */
    val status: String,
    val createdDate: Long,
    val decidedDate: Long? = null,
)

@Entity(tableName = "parent_settings")
data class ParentSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val pinSalt: String,
    val consentAt: Long,
)

object TxType {
    const val EARN = "EARN"
    const val BONUS = "BONUS"
    const val REDEEM = "REDEEM"
}

object RequestStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val DECLINED = "DECLINED"
}

/** Answers on the first try, grouped by operation. */
data class OperationCount(val operation: String, val answered: Int, val correctFirstTry: Int)

data class WeekTotals(val answered: Int, val correctFirstTry: Int)
