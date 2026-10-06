package com.mathsquest.app.di

import android.content.Context
import androidx.room.Room
import com.mathsquest.app.data.local.MathsQuestDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MathsQuestDatabase =
        Room.databaseBuilder(context, MathsQuestDatabase::class.java, "maths-quest.db").build()

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()
}
