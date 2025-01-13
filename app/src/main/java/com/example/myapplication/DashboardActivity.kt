package com.example.myapplication;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.Adapter.ChangeDetailAdapter
import com.example.myapplication.Adapter.ChangeDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class DashboardActivity : AppCompatActivity() {

    private lateinit var teacherNameTextView: TextView
    private lateinit var backButton: Button
    private lateinit var changesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        teacherNameTextView = findViewById(R.id.teacherNameTextView)
        backButton = findViewById(R.id.backButton)
        changesRecyclerView = findViewById(R.id.recyclerView)

        changesRecyclerView.layoutManager = LinearLayoutManager(this)

        val teacherName = intent.getStringExtra("teacher_name")
        val teacherCode = extractTeacherCode(teacherName ?: "")

        teacherNameTextView.text = teacherName

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("from_dashboard", true)
            startActivity(intent)
            finish()
        }

        fetchAndDisplayChanges(teacherCode)
    }

    private fun extractTeacherCode(teacherName: String): String {
        return teacherName.substringAfter("(").substringBefore(")").trim()
    }

    private fun fetchAndDisplayChanges(teacherCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val changes = fetchChangesFromWeb(teacherCode)
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
            val url = "https://bakalar.pslib.cz/rodice/Timetable/Public/Actual/Teacher/$teacherCode"
            val doc = Jsoup.connect(url).get()
            val elements = doc.select(".pink")

            elements.map { element ->
                val detail = element.attr("data-detail")
                ChangeDetail(
                    type = detail.substringAfter("&quot;type&quot;:&quot;").substringBefore("&quot;"),
                    subjectText = detail.substringAfter("&quot;subjecttext&quot;:&quot;").substringBefore("&quot;"),
                    absentInfo = detail.substringAfter("&quot;absentinfo&quot;:&quot;").substringBefore("&quot;"),
                    infoAbsentName = detail.substringAfter("&quot;InfoAbsentName&quot;:")
                        .substringBefore(",").removeSurrounding("\"", "\""),
                    removedInfo = detail.substringAfter("&quot;removedinfo&quot;:&quot;").substringBefore("&quot;")
                )
            }
        }

        catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateChangesList(changes: List<ChangeDetail>) {
        if (changes.isNotEmpty()) {
            val adapter = ChangeDetailAdapter(changes)
            changesRecyclerView.adapter = adapter
        } else {
            teacherNameTextView.text = getString(R.string.no_changes)
        }
    }
}
