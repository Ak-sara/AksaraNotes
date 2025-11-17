package com.aksara.notes.ui.database.builder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.aksara.notes.R

class IconPickerAdapter(
    private val context: Context,
    private val icons: Array<String>,
    private val onIconSelected: (String) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = icons.size

    override fun getItem(position: Int): String = icons[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_icon_picker, parent, false)

        val iconTextView = view.findViewById<TextView>(R.id.tv_icon)
        val icon = icons[position]

        iconTextView.text = icon
        iconTextView.setOnClickListener {
            onIconSelected(icon)
        }

        return view
    }
}
