package com.mathsquest.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY createdAt")
    fun observeAll(): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE id = :id")
    fun observe(id: Long): Flow<ChildEntity?>

    @Query("SELECT * FROM children WHERE id = :id")
    suspend fun get(id: Long): ChildEntity?

    @Query("SELECT COUNT(*) FROM children WHERE name = :name")
    suspend fun countNamed(name: String): Int

    @Query("SELECT COUNT(*) FROM children")
    suspend fun count(): Int

    @Insert
    suspend fun insert(child: ChildEntity): Long

    @Update
    suspend fun update(child: ChildEntity)
}

/** Insert-only by design: the ledger is append-only. */
@Dao
interface CoinDao {
    @Insert
    suspend fun insert(tx: CoinTransactionEntity): Long

    @Query("SELECT COALESCE(SUM(coins), 0) FROM coin_transactions WHERE childId = :childId")
    fun observeBalance(childId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(coins), 0) FROM coin_transactions WHERE childId = :childId")
    suspend fun balance(childId: Long): Int

    @Query("SELECT COALESCE(SUM(coins), 0) FROM coin_transactions WHERE childId = :childId AND coins > 0 AND createdDate >= :since")
    fun observeEarnedSince(childId: Long, since: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(coins), 0) FROM coin_transactions WHERE childId = :childId AND coins > 0 AND createdDate >= :since")
    suspend fun earnedSince(childId: Long, since: Long): Int

    /** Coins earned through play since [since]; excludes opening balances. */
    @Query("SELECT COALESCE(SUM(coins), 0) FROM coin_transactions WHERE childId = :childId AND coins > 0 AND transactionType != 'OPENING' AND createdDate >= :since")
    fun observePlayEarnedSince(childId: Long, since: Long): Flow<Int>

    @Query("SELECT * FROM coin_transactions WHERE childId = :childId ORDER BY createdDate DESC, transactionId DESC LIMIT :limit")
    fun observeRecent(childId: Long, limit: Int): Flow<List<CoinTransactionEntity>>
}

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AnswerAttemptEntity)

    @Insert
    suspend fun insertAll(attempts: List<AnswerAttemptEntity>)

    @Query(
        "SELECT operation, COUNT(*) AS answered, COALESCE(SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END), 0) AS correctFirstTry " +
            "FROM answer_attempts WHERE childId = :childId AND attempt = 1 GROUP BY operation",
    )
    fun observeOperationCounts(childId: Long): Flow<List<OperationCount>>

    @Query(
        "SELECT COUNT(*) AS answered, COALESCE(SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END), 0) AS correctFirstTry " +
            "FROM answer_attempts WHERE childId = :childId AND attempt = 1 AND createdDate >= :since",
    )
    fun observeWeek(childId: Long, since: Long): Flow<WeekTotals>

    @Query("SELECT COALESCE(SUM(elapsedMillis), 0) FROM answer_attempts WHERE childId = :childId AND createdDate >= :since")
    fun observeStudyMillis(childId: Long, since: Long): Flow<Long>
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE active = 1 ORDER BY cost")
    fun observeActive(): Flow<List<RewardEntity>>

    @Query("SELECT COUNT(*) FROM rewards")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(rewards: List<RewardEntity>)

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun get(id: Long): RewardEntity?

    @Query("SELECT * FROM rewards WHERE title = :title LIMIT 1")
    suspend fun byTitle(title: String): RewardEntity?
}

@Dao
interface RequestDao {
    @Insert
    suspend fun insert(request: RewardRequestEntity): Long

    @Update
    suspend fun update(request: RewardRequestEntity)

    @Query("SELECT * FROM reward_requests WHERE id = :id")
    suspend fun get(id: Long): RewardRequestEntity?

    @Query("SELECT * FROM reward_requests WHERE childId = :childId ORDER BY createdDate DESC LIMIT 10")
    fun observeForChild(childId: Long): Flow<List<RewardRequestEntity>>

    @Query("SELECT * FROM reward_requests WHERE status = 'PENDING' ORDER BY createdDate")
    fun observePending(): Flow<List<RewardRequestEntity>>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM reward_requests WHERE childId = :childId AND status = 'PENDING'")
    fun observeHeld(childId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM reward_requests WHERE childId = :childId AND status = 'PENDING'")
    suspend fun held(childId: Long): Int
}

@Dao
interface ParentDao {
    @Query("SELECT * FROM parent_settings WHERE id = 1")
    suspend fun get(): ParentSettingsEntity?

    @Query("SELECT COUNT(*) > 0 FROM parent_settings")
    fun observeIsSetUp(): Flow<Boolean>

    @Upsert
    suspend fun upsert(settings: ParentSettingsEntity)
}
