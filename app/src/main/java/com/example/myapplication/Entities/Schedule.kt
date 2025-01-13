package com.example.Entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "schedule_changes")
data class ScheduleChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: String, // FK na učitele
    val startDate: String, // Začátek hodiny
    val endDate: String,   // Konec hodiny
    val subject: String,   // Předmět
    val className: String  // Třída
)

@Dao
interface ScheduleChangeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleChanges(scheduleChanges: List<ScheduleChange>)

    @Query("SELECT * FROM schedule_changes WHERE teacherId = :teacherId")
    suspend fun getScheduleChangesByTeacher(teacherId: String): List<ScheduleChange>
}