package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.Entities.ScheduleChange
import com.example.Entities.ScheduleChangeDao
import com.example.myapplication.Adapter.ChangeDetail
import org.json.JSONException
import org.json.JSONObject

class ScheduleChangeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val teacherCode = inputData.getString("teacherCode") ?: return@withContext Result.failure()
                val dao = AppDatabaseSingleton.getInstance(applicationContext).scheduleChangeDao()

                updateDatabaseWithChanges(teacherCode, dao)

                val changes = dao.getScheduleChangesByTeacher(teacherCode)
                if (changes.isNotEmpty()) {
                    sendNotification(
                        "Rozvrh byl aktualizován!",
                        "Bylo nalezeno ${changes.size} změn."
                    )
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }


    private fun sendNotification(title: String, content: String) {
        val channelId = "schedule_update_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Schedule Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.zdrahalabigbrother)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(applicationContext).notify(2, notification)
    }

    private suspend fun updateDatabaseWithChanges(teacherCode: String, dao: ScheduleChangeDao) {
        val newChanges = fetchChangesFromWeb(teacherCode)
        val existingChanges = dao.getScheduleChangesByTeacher(teacherCode)

        val changesToAdd = newChanges.filter { newChange ->
            existingChanges.none { existingChange ->
                existingChange.date == newChange.date &&
                        existingChange.hour == newChange.hour &&
                        existingChange.subject == newChange.subject &&
                        existingChange.changeInfo == newChange.changeInfo
            }
        }

        if (changesToAdd.isNotEmpty()) {
            dao.insertScheduleChanges(changesToAdd.map { change ->
                ScheduleChange(
                    teacherId = teacherCode,
                    date = change.date,
                    hour = change.hour,
                    subject = change.subject,
                    className = change.className,
                    room = change.room,
                    changeInfo = change.changeInfo
                )
            })
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
}

