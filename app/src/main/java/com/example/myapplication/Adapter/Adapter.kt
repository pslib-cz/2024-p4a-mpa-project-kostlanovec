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
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val hourTextView: TextView = view.findViewById(R.id.hourTextView)
        val classNameTextView: TextView = view.findViewById(R.id.classNameTextView)
        val subjectTextView: TextView = view.findViewById(R.id.subjectTextView)
        val roomTextView: TextView = view.findViewById(R.id.roomTextView)
        val changeInfoTextView: TextView = view.findViewById(R.id.changeInfoTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_change_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val change = changes[position]
        holder.dateTextView.text = change.date
        holder.hourTextView.text = change.hour
        holder.classNameTextView.text = change.className
        holder.subjectTextView.text = change.subject
        holder.roomTextView.text = change.room
        holder.changeInfoTextView.text = change.changeInfo
    }

    override fun getItemCount(): Int = changes.size
}

// Updated ChangeDetail data class

data class ChangeDetail(
    val date: String,
    val hour: String,
    val subject: String,
    val className: String,
    val room: String,
    val changeInfo: String
)
