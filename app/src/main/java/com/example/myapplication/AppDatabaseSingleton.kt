package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.Entities.ScheduleChange
import com.example.Entities.ScheduleChangeDao
import com.example.Entities.Teacher
import com.example.Entities.TeacherDao

class AppDatabaseSingleton private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Database(
    entities = [Teacher::class, ScheduleChange::class],
    version = 2
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun scheduleChangeDao(): ScheduleChangeDao
}