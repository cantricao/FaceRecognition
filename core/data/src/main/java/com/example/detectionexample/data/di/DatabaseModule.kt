package com.example.detectionexample.data.di

import android.content.Context
import androidx.room.Room
import com.example.detectionexample.data.local.FaceRecognitionDatabase
import com.example.detectionexample.data.local.PersonDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FaceRecognitionDatabase {
        return Room.databaseBuilder(
            context,
            FaceRecognitionDatabase::class.java,
            FaceRecognitionDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun providePersonDao(database: FaceRecognitionDatabase): PersonDao {
        return database.personDao()
    }
}
