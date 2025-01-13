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

class ScheduleChangeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect("https://bakalar.pslib.cz/rodice/Timetable/Public/").get()
                val updatedContent = doc.body().text() // Tady přizpůsobte výběr
                val sharedPreferences =
                    applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val lastContent = sharedPreferences.getString("last_schedule", "")

                if (lastContent != updatedContent) {
                    sharedPreferences.edit().putString("last_schedule", updatedContent).apply()
                    sendNotification("Rozvrh byl aktualizován!", "Podívejte se na změny.")
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
                applicationContext, // Opraveno
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(applicationContext).notify(2, notification)
    }

}