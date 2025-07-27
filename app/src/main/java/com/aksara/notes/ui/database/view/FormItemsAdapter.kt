package com.aksara.notes.ui.database.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aksara.notes.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import com.aksara.notes.data.database.entities.Form
import java.util.Date

class FormItemsAdapter(
    private val onFormClick: (Form) -> Unit
) : ListAdapter<Form, FormItemsAdapter.FormViewHolder>(FormDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_form, parent, false)
        return FormViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FormViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val formTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val formSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)
        private val formDate: TextView = itemView.findViewById(R.id.tv_item_date)

        fun bind(form: Form) {
            // Parse JSON data
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = try {
                gson.fromJson(form.data, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            // Display first field as title, second as subtitle
            val values = data.values.toList()
            formTitle.text = when {
                values.isNotEmpty() -> values[0].toString()
                else -> "Untitled Form"
            }

            formSubtitle.text = when {
                values.size > 1 -> values[1].toString()
                else -> "No details"
            }

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formDate.text = dateFormat.format(Date(form.updatedAt))

            itemView.setOnClickListener { onFormClick(form) }
        }
    }

    class FormDiffCallback : DiffUtil.ItemCallback<Form>() {
        override fun areItemsTheSame(oldForm: Form, newForm: Form): Boolean {
            return oldForm.id == newForm.id
        }

        override fun areContentsTheSame(oldForm: Form, newForm: Form): Boolean {
            return oldForm == newForm
        }
    }
}