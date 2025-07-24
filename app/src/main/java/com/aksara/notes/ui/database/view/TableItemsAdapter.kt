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
import com.aksara.notes.data.database.entities.TableItem
import java.util.Date

class TableItemsAdapter(
    private val onItemClick: (TableItem) -> Unit
) : ListAdapter<TableItem, TableItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        private val itemSubtitle: TextView = itemView.findViewById(R.id.tv_item_subtitle)
        private val itemDate: TextView = itemView.findViewById(R.id.tv_item_date)

        fun bind(item: TableItem) {
            // Parse JSON data
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = try {
                gson.fromJson(item.data, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            // Display first field as title, second as subtitle
            val values = data.values.toList()
            itemTitle.text = when {
                values.isNotEmpty() -> values[0].toString()
                else -> "Untitled Item"
            }

            itemSubtitle.text = when {
                values.size > 1 -> values[1].toString()
                else -> "No details"
            }

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            itemDate.text = dateFormat.format(Date(item.updatedAt))

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<TableItem>() {
        override fun areItemsTheSame(oldItem: TableItem, newItem: TableItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TableItem, newItem: TableItem): Boolean {
            return oldItem == newItem
        }
    }
}