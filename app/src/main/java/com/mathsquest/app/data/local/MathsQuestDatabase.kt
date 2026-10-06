package com.mathsquest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChildEntity::class,
        CoinTransactionEntity::class,
        AnswerAttemptEntity::class,
        RewardEntity::class,
        RewardRequestEntity::class,
        ParentSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MathsQuestDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun coinDao(): CoinDao
    abstract fun attemptDao(): AttemptDao
    abstract fun rewardDao(): RewardDao
    abstract fun requestDao(): RequestDao
    abstract fun parentDao(): ParentDao
}
