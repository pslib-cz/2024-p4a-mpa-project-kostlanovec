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
            // Pokud instance již existuje, vrátíme ji
            return INSTANCE ?: synchronized(this) {
                // Vytvoření nové instance, pokud neexistuje
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // Název databáze
                )
                    .fallbackToDestructiveMigration() // Zničí starou databázi při změně schématu, pokud nejsou migrace
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Database(
    entities = [Teacher::class, ScheduleChange::class],
    version = 1
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun scheduleChangeDao(): ScheduleChangeDao
}