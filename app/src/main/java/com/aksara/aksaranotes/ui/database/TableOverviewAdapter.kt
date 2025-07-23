package com.aksara.aksaranotes.ui.database
import android.util.Log
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aksara.aksaranotes.R
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.ui.database.builder.TableBuilderActivity
import com.google.android.material.button.MaterialButton

class TableOverviewAdapter(
    private val onTableClick: (CustomTable) -> Unit,
    private val onCreateItemClick: (CustomTable) -> Unit,
    private val onDeleteTable: (CustomTable) -> Unit  // ADD THIS PARAMETER
) : ListAdapter<TableOverviewItem, TableOverviewAdapter.TableViewHolder>(TableDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table_overview, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tableIcon: TextView = itemView.findViewById(R.id.tv_table_icon)
        private val tableName: TextView = itemView.findViewById(R.id.tv_table_name)
        private val tableDescription: TextView = itemView.findViewById(R.id.tv_table_description)
        private val itemCount: TextView = itemView.findViewById(R.id.tv_item_count)
        private val tableType: TextView = itemView.findViewById(R.id.tv_table_type)
        private val addButton: MaterialButton = itemView.findViewById(R.id.btn_add_item)

        fun bind(item: TableOverviewItem) {
            val table = item.table

            tableIcon.text = table.icon
            tableName.text = table.name
            tableDescription.text = table.description.ifEmpty { "No description" }
            itemCount.text = "${item.itemCount} items"
            tableType.text = table.tableType.replaceFirstChar { it.uppercase() }

            itemView.setOnClickListener { onTableClick(table) }
            addButton.setOnClickListener { onCreateItemClick(table) }

            // Long press to edit table structure
            itemView.setOnLongClickListener {
                showTableOptionsDialog(table)
                true
            }
        }

        // In the showTableOptionsDialog method:

        private fun showTableOptionsDialog(table: CustomTable) {
            Log.d("TableOverview", "showTableOptionsDialog called for table: ${table.name}")

            val options = arrayOf("Edit Structure", "Delete Table")

            AlertDialog.Builder(itemView.context)
                .setTitle("${table.icon} ${table.name}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Edit table structure
                            Log.d("TableOverview", "Edit Structure selected for table ID: ${table.id}")

                            try {
                                val intent = Intent(itemView.context, TableBuilderActivity::class.java)
                                intent.putExtra("edit_table_id", table.id)
                                intent.putExtra("edit_table_name", table.name)

                                Log.d("TableOverview", "Starting TableBuilderActivity with intent: $intent")
                                Log.d("TableOverview", "Intent extras: edit_table_id=${intent.getStringExtra("edit_table_id")}")

                                itemView.context.startActivity(intent)
                                Log.d("TableOverview", "Activity started successfully")

                            } catch (e: Exception) {
                                Log.e("TableOverview", "Error starting TableBuilderActivity", e)
                                Toast.makeText(itemView.context, "Error opening editor: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        1 -> {
                            // Delete table
                            showDeleteConfirmation(table)
                        }
                    }
                }
                .show()
        }

        private fun showDeleteConfirmation(table: CustomTable) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Table")
                .setMessage("Are you sure you want to delete '${table.name}' and all its ${itemCount.text}?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteTable(table)  // Call the delete callback
                    Toast.makeText(
                        itemView.context,
                        "Table '${table.name}' deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class TableDiffCallback : DiffUtil.ItemCallback<TableOverviewItem>() {
        override fun areItemsTheSame(oldItem: TableOverviewItem, newItem: TableOverviewItem): Boolean {
            return oldItem.table.id == newItem.table.id
        }

        override fun areContentsTheSame(oldItem: TableOverviewItem, newItem: TableOverviewItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Data class to hold table + item count
data class TableOverviewItem(
    val table: CustomTable,
    val itemCount: Int
)