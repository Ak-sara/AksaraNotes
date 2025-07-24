package com.aksara.notes.ui.database.builder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aksara.notes.R
import com.aksara.notes.data.models.TableColumn

class ColumnBuilderAdapter(
    private val onColumnEdit: (TableColumn) -> Unit,
    private val onColumnDelete: (TableColumn) -> Unit
) : ListAdapter<TableColumn, ColumnBuilderAdapter.ColumnViewHolder>(ColumnDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_column_builder, parent, false)
        return ColumnViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColumnViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val columnIcon: TextView = itemView.findViewById(R.id.tv_column_icon)
        private val columnName: TextView = itemView.findViewById(R.id.tv_column_name)
        private val columnType: TextView = itemView.findViewById(R.id.tv_column_type)
        private val requiredIndicator: TextView = itemView.findViewById(R.id.tv_required_indicator)
        private val editButton: ImageButton = itemView.findViewById(R.id.btn_edit_column)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_column)

        fun bind(column: TableColumn) {
            columnIcon.text = column.type.icon
            columnName.text = column.name
            columnType.text = column.type.displayName

            requiredIndicator.visibility = if (column.required) View.VISIBLE else View.GONE

            editButton.setOnClickListener { onColumnEdit(column) }
            deleteButton.setOnClickListener { onColumnDelete(column) }
        }
    }

    class ColumnDiffCallback : DiffUtil.ItemCallback<TableColumn>() {
        override fun areItemsTheSame(oldItem: TableColumn, newItem: TableColumn): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TableColumn, newItem: TableColumn): Boolean {
            return oldItem == newItem
        }
    }
}