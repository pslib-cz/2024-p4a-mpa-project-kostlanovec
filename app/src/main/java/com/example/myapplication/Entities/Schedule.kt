package com.example.Entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "schedule_changes")
data class ScheduleChange(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teacherId: String,
    val date: String,
    val hour: String,
    val subject: String,
    val className: String,
    val room: String,
    val changeInfo: String
)


@Dao
interface ScheduleChangeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleChanges(scheduleChanges: List<ScheduleChange>)

    @Query("SELECT * FROM schedule_changes WHERE teacherId = :teacherId")
    suspend fun getScheduleChangesByTeacher(teacherId: String): List<ScheduleChange>

    @Query("DELETE FROM schedule_changes WHERE teacherId = :teacherId")
    suspend fun deleteChangesForTeacher(teacherId: String)
}
