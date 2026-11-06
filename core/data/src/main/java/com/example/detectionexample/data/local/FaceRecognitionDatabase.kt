package com.example.detectionexample.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PersonEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FaceRecognitionDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao

    companion object {
        const val DATABASE_NAME = "face_recognition.db"
    }
}
