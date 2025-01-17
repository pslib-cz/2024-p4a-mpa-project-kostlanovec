package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.Entities.ScheduleChange
import com.example.myapplication.Adapter.ChangeDetailAdapter
import com.example.myapplication.Adapter.ChangeDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var teacherNameTextView: TextView
    private lateinit var backButton: Button
    private lateinit var currentWeekButton: Button
    private lateinit var nextWeekButton: Button
    private lateinit var changesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        teacherNameTextView = findViewById(R.id.teacherNameTextView)
        backButton = findViewById(R.id.backButton)
        currentWeekButton = findViewById(R.id.currentWeekButton)
        nextWeekButton = findViewById(R.id.nextWeekButton)
        changesRecyclerView = findViewById(R.id.recyclerView)

        changesRecyclerView.layoutManager = LinearLayoutManager(this)

        val teacherId = intent.getStringExtra("teacher_id")

        if (teacherId != null) {
            fetchTeacherNameAndSetup(teacherId)
            fetchAndDisplayChanges(teacherId)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("from_dashboard", true)
            startActivity(intent)
            finish()
        }

        currentWeekButton.setOnClickListener {
            openWebPage("https://bakalar.pslib.cz/rodice/Timetable/Public/Actual/Teacher/$teacherId")
        }

        nextWeekButton.setOnClickListener {
            openWebPage("https://bakalar.pslib.cz/rodice/Timetable/Public/Next/Teacher/$teacherId")
        }

        scheduleBackgroundWork()
    }

    private fun fetchTeacherNameAndSetup(teacherId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val teacherDao = AppDatabaseSingleton.getInstance(applicationContext).teacherDao()
            val teacher = teacherDao.getTeacherById(teacherId)

            withContext(Dispatchers.Main) {
                teacherNameTextView.text = teacher?.name ?: getString(R.string.unknown_teacher)
            }
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse(url)
        startActivity(intent)
    }

    private fun fetchAndDisplayChanges(teacherCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val changes = fetchChangesFromWeb(teacherCode)
                saveChangesToDatabase(changes, teacherCode)
                withContext(Dispatchers.Main) {
                    updateChangesList(changes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchChangesFromWeb(teacherCode: String): List<ChangeDetail> {
        return try {
            val urls = listOf(
                "https://bakalar.pslib.cz/rodice/Timetable/Public/Actual/Teacher/$teacherCode",
                "https://bakalar.pslib.cz/rodice/Timetable/Public/Next/Teacher/$teacherCode"
            )

            val allChanges = mutableListOf<ChangeDetail>()

            for (url in urls) {
                try {
                    val doc = Jsoup.connect(url).get()
                    val elements = doc.select(".pink")

                    val changes = elements.mapNotNull { element ->
                        val detail = element.attr("data-detail")
                        try {
                            val jsonObject = JSONObject(detail.replace("&quot;", "\""))
                            val type = jsonObject.optString("type", "")
                            val subjectText = jsonObject.optString("subjecttext", "")
                            val room = jsonObject.optString("room", "")
                            val group = jsonObject.optString("group", "")
                            var changeInfo = jsonObject.optString("changeinfo", "")

                            if (type == "removed") {
                                changeInfo = "Odpadává hodina"
                            }

                            val (subject, dateTime) = if ("|" in subjectText) {
                                subjectText.split("|").map { it.trim() }
                            } else {
                                listOf("", "")
                            }

                            val date = dateTime.substringBefore(" ")
                            val hour = dateTime.substringBefore(" ") + " " + dateTime.substringAfter(" ").trim()

                            ChangeDetail(
                                date = date,
                                hour = hour,
                                subject = subject,
                                className = group,
                                room = room,
                                changeInfo = changeInfo
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            null
                        }
                    }

                    allChanges.addAll(changes)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            allChanges
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateChangesList(changes: List<ChangeDetail>) {
        if (changes.isNotEmpty()) {
            val adapter = ChangeDetailAdapter(changes)
            changesRecyclerView.adapter = adapter
        }
    }

    private fun scheduleBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduleChangeWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }


    private suspend fun saveChangesToDatabase(changes: List<ChangeDetail>, teacherId: String) {
        val scheduleChanges = changes.map { change ->
            ScheduleChange(
                teacherId = teacherId,
                date = change.date,
                hour = change.hour,
                subject = change.subject,
                className = change.className,
                room = change.room,
                changeInfo = change.changeInfo
            )
        }

        val changeDao = AppDatabaseSingleton.getInstance(applicationContext).scheduleChangeDao()
        changeDao.insertScheduleChanges(scheduleChanges)
    }
}