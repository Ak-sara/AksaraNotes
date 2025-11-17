package com.aksara.notes.ui.database
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
import com.aksara.notes.R
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.ui.database.builder.DatasetBuilderActivity
import com.google.android.material.button.MaterialButton

class DatasetOverviewAdapter(
    private val onDatasetClick: (Dataset) -> Unit,
    private val onCreateFormClick: (Dataset) -> Unit,
    private val onDeleteDataset: (Dataset) -> Unit
) : ListAdapter<DatasetOverviewItem, DatasetOverviewAdapter.DatasetViewHolder>(DatasetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DatasetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dataset_overview, parent, false)
        return DatasetViewHolder(view)
    }

    override fun onBindViewHolder(holder: DatasetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DatasetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val datasetIcon: TextView = itemView.findViewById(R.id.tv_table_icon)
        private val datasetName: TextView = itemView.findViewById(R.id.tv_table_name)
        private val datasetDescription: TextView = itemView.findViewById(R.id.tv_table_description)
        private val formCount: TextView = itemView.findViewById(R.id.tv_item_count)
        private val datasetType: TextView = itemView.findViewById(R.id.tv_table_type)
        private val menuButton: MaterialButton = itemView.findViewById(R.id.btn_menu)

        fun bind(item: DatasetOverviewItem) {
            val dataset = item.dataset

            datasetIcon.text = dataset.icon
            datasetName.text = dataset.name
            datasetDescription.text = dataset.description.ifEmpty { "No description" }
            formCount.text = "${item.formCount} forms"
            datasetType.text = dataset.datasetType.replaceFirstChar { it.uppercase() }

            // Card click opens dataset data view
            itemView.setOnClickListener { onDatasetClick(dataset) }

            // Menu button shows options (Edit Structure / Delete)
            menuButton.setOnClickListener {
                showDatasetOptionsDialog(dataset)
            }
        }

        private fun showDatasetOptionsDialog(dataset: Dataset) {
            Log.d("DatasetOverview", "showDatasetOptionsDialog called for dataset: ${dataset.name}")

            val options = arrayOf("Edit Structure", "Delete Dataset")

            AlertDialog.Builder(itemView.context)
                .setTitle("${dataset.icon} ${dataset.name}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Edit dataset structure
                            Log.d("DatasetOverview", "Edit Structure selected for dataset ID: ${dataset.id}")

                            try {
                                val intent = Intent(itemView.context, DatasetBuilderActivity::class.java)
                                intent.putExtra("edit_dataset_id", dataset.id)
                                intent.putExtra("edit_dataset_name", dataset.name)

                                Log.d("DatasetOverview", "Starting DatasetBuilderActivity with intent: $intent")
                                Log.d("DatasetOverview", "Intent extras: edit_dataset_id=${intent.getStringExtra("edit_dataset_id")}")

                                itemView.context.startActivity(intent)
                                Log.d("DatasetOverview", "Activity started successfully")

                            } catch (e: Exception) {
                                Log.e("DatasetOverview", "Error starting DatasetBuilderActivity", e)
                                Toast.makeText(itemView.context, "Error opening editor: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        1 -> {
                            // Delete dataset
                            showDeleteConfirmation(dataset)
                        }
                    }
                }
                .show()
        }

        private fun showDeleteConfirmation(dataset: Dataset) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Dataset")
                .setMessage("Are you sure you want to delete '${dataset.name}' and all its ${formCount.text}?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteDataset(dataset)  // Call the delete callback
                    Toast.makeText(
                        itemView.context,
                        "Dataset '${dataset.name}' deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class DatasetDiffCallback : DiffUtil.ItemCallback<DatasetOverviewItem>() {
        override fun areItemsTheSame(oldItem: DatasetOverviewItem, newItem: DatasetOverviewItem): Boolean {
            return oldItem.dataset.id == newItem.dataset.id
        }

        override fun areContentsTheSame(oldItem: DatasetOverviewItem, newItem: DatasetOverviewItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Data class to hold dataset + form count
data class DatasetOverviewItem(
    val dataset: Dataset,
    val formCount: Int
)