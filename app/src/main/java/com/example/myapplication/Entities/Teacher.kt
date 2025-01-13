package com.example.Entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val id: String,
    val name: String
)

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers:List<Teacher>)

    @Query("SELECT * FROM teachers")
    suspend fun getAllTeachers(): List<Teacher>
}
