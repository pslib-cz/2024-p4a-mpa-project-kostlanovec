package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.Entities.Teacher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var teacherSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var updateTeachers: Button
    private lateinit var timeListView: ListView
    private lateinit var addTimeButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        teacherSpinner = findViewById(R.id.teacherSpinner)
        updateTeachers = findViewById(R.id.button)
        saveButton = findViewById(R.id.button2)
        timeListView = findViewById(R.id.timeListView)
        addTimeButton = findViewById(R.id.addTimeButton)
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val fromDashboard = intent.getBooleanExtra("from_dashboard", false)

        if (!fromDashboard) {
            val savedTeacher = sharedPreferences.getString("teacher_id", null)
            if (savedTeacher != null) {
                navigateToDashboard(savedTeacher)
                return
            }
        }

        setupTeachers()
        saveButton.setOnClickListener {
            saveSelectedTeacher()
        }
        updateTeachers.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val teacherNames = fetchTeachersFromWeb()
                    withContext(Dispatchers.Main) {
                        updateSpinner(teacherNames)
                        sendUpdateNotification()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        addTimeButton.setOnClickListener {
            showTimePickerDialog(null)
        }

        timeListView.setOnItemClickListener { _, _, position, _ ->
            val times = getSavedTimes()
            showTimePickerDialog(times[position]) { updatedTime ->
                val updatedTimes = times.toMutableList()
                updatedTimes[position] = updatedTime
                saveTimes(updatedTimes.toSet())
                updateTimeListView()
            }
        }

        timeListView.setOnItemLongClickListener { _, _, position, _ ->
            val times = getSavedTimes().toMutableList()
            times.removeAt(position)
            saveTimes(times.toSet())
            updateTimeListView()
            true
        }

        updateTimeListView()
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
                    .asSequence()
                    .filter { element -> element.hasAttr("value") && element.attr("value").isNotEmpty() }
                    .map { element -> Teacher(element.attr("value"), element.text()) }
                    .toList()

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
        val savedTimes = getSavedTimes()

        if (savedTimes.isEmpty()) {
            android.widget.Toast.makeText(
                this,
                "Nejdříve nastavte alespoň jeden čas na notifikace.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedTeacher = teacherSpinner.selectedItem?.toString()
        if (selectedTeacher != null) {
            val teacherId = selectedTeacher.substringAfter("(").substringBefore(")").trim()
            sharedPreferences.edit()
                .putString("teacher_id", teacherId)
                .apply()

            navigateToDashboard(teacherId)
        }
    }


    private fun navigateToDashboard(teacherId: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("teacher_id", teacherId)
        startActivity(intent)
        finish()
    }

    private fun sendUpdateNotification() {
        val channelId = "teacher_update_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Teacher Update Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.zdrahalabigbrother)
            .setContentTitle("Aktualizace učitelů")
            .setContentText("Seznam učitelů byl úspěšně aktualizován.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(1, notification)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }

    private fun getSavedTimes(): List<String> {
        return sharedPreferences.getStringSet("notification_times", emptySet())!!.toList()
    }

    private fun saveTimes(times: Set<String>) {
        sharedPreferences.edit()
            .putStringSet("notification_times", times)
            .apply()
    }

    private fun updateTimeListView() {
        val times = getSavedTimes()
            .sortedBy { parseTimeToMinutes(it) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, times)
        timeListView.adapter = adapter
    }

    @SuppressLint("DefaultLocale")
    private fun showTimePickerDialog(existingTime: String?, onTimeSelected: ((String) -> Unit)? = null) {
        val calendar = java.util.Calendar.getInstance()
        if (existingTime != null) {
            val parts = existingTime.split(":")
            calendar.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(java.util.Calendar.MINUTE, parts[1].toInt())
        }

        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            val times = getSavedTimes().toMutableSet()

            if (onTimeSelected != null) {
                onTimeSelected(formattedTime)
            } else {
                times.add(formattedTime)
                saveTimes(times)
                updateTimeListView()
            }
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun parseTimeToMinutes(time: String): Int {
        val (hours, minutes) = time.split(":").map { it.toInt() }
        return hours * 60 + minutes
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendUpdateNotification()
            } else {
                println("Permission for notifications was denied.")
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
}