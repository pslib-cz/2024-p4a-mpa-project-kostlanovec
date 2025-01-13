package com.example.myapplication.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ChangeDetailAdapter(private val changes: List<ChangeDetail>) :
    RecyclerView.Adapter<ChangeDetailAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeTextView: TextView = view.findViewById(R.id.typeTextView)
        val subjectTextView: TextView = view.findViewById(R.id.subjectTextView)
        val absentInfoTextView: TextView = view.findViewById(R.id.absentInfoTextView)
        val infoAbsentNameTextView: TextView = view.findViewById(R.id.infoAbsentNameTextView)
        val removedInfoTextView: TextView = view.findViewById(R.id.removedInfoTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_change_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val change = changes[position]
        holder.typeTextView.text = "Type: ${change.type}"
        holder.subjectTextView.text = "Subject: ${change.subjectText}"
        holder.absentInfoTextView.text = "Absent Info: ${change.absentInfo}"
        holder.infoAbsentNameTextView.text = "Info Absent Name: ${change.infoAbsentName}"
        holder.removedInfoTextView.text = "Removed Info: ${change.removedInfo}"
    }

    override fun getItemCount(): Int = changes.size
}

data class ChangeDetail(
    val type: String,
    val subjectText: String,
    val absentInfo: String,
    val infoAbsentName: String,
    val removedInfo: String
)