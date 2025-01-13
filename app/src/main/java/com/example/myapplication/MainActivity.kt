package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.lifecycleScope;
import com.example.Entities.Teacher
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;
import org.jsoup.Jsoup;

class MainActivity : AppCompatActivity() {

    private lateinit var teacherSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        teacherSpinner = findViewById(R.id.teacherSpinner)
        saveButton = findViewById(R.id.button2)
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val fromDashboard = intent.getBooleanExtra("from_dashboard", false)

        if (!fromDashboard) {
            val savedTeacher = sharedPreferences.getString("selected_teacher", null)
            if (savedTeacher != null) {
                navigateToDashboard(savedTeacher)
                return
            }
        }

        setupTeachers()
        saveButton.setOnClickListener {
            saveSelectedTeacher()
        }
    }

    private fun setupTeachers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val teacherNames = fetchTeachersFromWeb()
                withContext(Dispatchers.Main) {
                    updateSpinner(teacherNames)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchTeachersFromWeb(): List<String> {
        return try {
            val teacherDao = AppDatabaseSingleton.getInstance(applicationContext).teacherDao()
            val dbTeachers = teacherDao.getAllTeachers()

            if (dbTeachers.isNotEmpty()) {
                dbTeachers.map { "${it.name} (${it.id})" }
            } else {
                val doc = Jsoup.connect("https://bakalar.pslib.cz/rodice/Timetable/Public/").get()
                val options = doc.select("#selectedTeacher option")
                val teacherList = options
                        .asSequence() // Použití sekvence pro efektivní filtrování a mapování
                    .filter { element -> element.hasAttr("value") && element.attr("value").isNotEmpty() }
                    .map { element -> Teacher(element.attr("value"), element.text()) }
                    .toList()

                // Uložení učitelů do databáze
                teacherDao.insertTeachers(teacherList)

                teacherList.map { "${it.name} (${it.id})" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateSpinner(teacherNames: List<String>) {
        if (teacherNames.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teacherNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            teacherSpinner.adapter = adapter
        } else {
            println("Spinner has no items to display.")
        }
    }

    private fun saveSelectedTeacher() {
        val selectedTeacher = teacherSpinner.selectedItem?.toString()
        if (selectedTeacher != null) {
            sharedPreferences.edit()
                .putString("selected_teacher", selectedTeacher)
                .apply()

            navigateToDashboard(selectedTeacher)
        }
    }

    private fun navigateToDashboard(teacherName: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("teacher_name", teacherName)
        startActivity(intent)
        finish()
    }
}
