package com.ahmad.quran.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.ahmad.quran.R
import com.ahmad.quran.data.Surah

class SurahAdapter(private val context: Context) : BaseAdapter() {
    private var items: List<Surah> = emptyList()
    private var types: Map<Int, String> = emptyMap()
    private val inflater = LayoutInflater.from(context)
    fun submit(items: List<Surah>, types: Map<Int, String>) {
        if (this.items === items && this.types === types) return
        this.items = items
        this.types = types
        notifyDataSetChanged()
    }
    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = getItem(position).surahNumber.toLong()
    override fun hasStableIds() = true
    private class Row(view: View) {
        val number: TextView = view.findViewById(R.id.surah_number)
        val name: TextView = view.findViewById(R.id.surah_name)
        val details: TextView = view.findViewById(R.id.surah_details)
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.row_surah, parent, false).apply { tag = Row(this) }
        val row = view.tag as Row
        val item = getItem(position)
        row.number.text = context.getString(R.string.number_value, item.surahNumber)
        row.name.text = item.name
        val type = when (types[item.surahNumber]) {
            "Meccan" -> context.getString(R.string.meccan)
            "Medinan" -> context.getString(R.string.medinan)
            else -> null
        }
        row.details.text = if (type == null) context.getString(R.string.surah_ayah_count, item.ayahCount)
            else context.getString(R.string.surah_type_count, type, item.ayahCount)
        return view
    }
}
