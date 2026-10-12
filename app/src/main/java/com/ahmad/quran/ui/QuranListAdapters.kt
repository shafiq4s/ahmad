package com.ahmad.quran.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import com.ahmad.quran.R
import com.ahmad.quran.data.Surah

class SurahAdapter(private val context: Context, private val firstPage: (Int) -> Int? = { null }) : BaseAdapter() {
    private var items: List<Surah> = emptyList()
    private var query = ""
    private var visibleItems: List<Surah> = emptyList()
    private var types: Map<Int, String> = emptyMap()
    private val inflater = LayoutInflater.from(context)
    fun submit(items: List<Surah>, types: Map<Int, String>) {
        if (this.items === items && this.types === types) return
        this.items = items
        this.types = types
        filter(query)
    }
    fun filter(value: String) {
        query = value
        val key = searchKey(value)
        visibleItems = if(key.isEmpty()) items else items.filter { searchKey(it.name).contains(key) }
        notifyDataSetChanged()
    }
    // Only surah names/search queries are folded; canonical ayah text is never touched.
    private fun searchKey(value: String): String = value.trim().replace(Regex("[\u064B-\u065F\u0670\u0640]"), "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    override fun getCount() = visibleItems.size
    override fun getItem(position: Int) = visibleItems[position]
    override fun getItemId(position: Int) = getItem(position).surahNumber.toLong()
    override fun hasStableIds() = true
    private class Row(view: View) {
        val number: TextView = view.findViewById(R.id.surah_number)
        val name: TextView = view.findViewById(R.id.surah_name)
        val details: TextView = view.findViewById(R.id.surah_details)
        val page: TextView = view.findViewById(R.id.surah_first_page)
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.row_surah, parent, false).apply { tag = Row(this) }
        val row = view.tag as Row
        val item = getItem(position)
        row.number.text = context.getString(R.string.number_value, item.surahNumber)
        row.name.text = item.name
        val page=firstPage(item.surahNumber)
        row.page.isVisible=page!=null
        row.page.text=page?.let { context.getString(R.string.surah_first_page,it) }.orEmpty()
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


class BookmarkAdapter(private val context: Context, private val onRemove: (ReadingBookmark) -> Unit) : BaseAdapter() {
    private var items: List<ReadingBookmark> = emptyList()
    private var names: Map<Int, String> = emptyMap()
    fun submit(items: List<ReadingBookmark>, surahs: List<Surah>) {
        if (this.items == items && names.size == surahs.size) return
        this.items=items.sortedByDescending { it.createdAtMillis };names=surahs.associate { it.surahNumber to it.name };notifyDataSetChanged()
    }
    override fun getCount()=items.size
    override fun getItem(position: Int)=items[position]
    override fun getItemId(position: Int)=items[position].pageNumber.toLong()
    override fun hasStableIds()=true
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view=convertView ?: LayoutInflater.from(context).inflate(R.layout.row_bookmark,parent,false)
        val item=getItem(position)
        view.findViewById<TextView>(R.id.bookmark_surah).text=context.getString(R.string.ayah_surah,names[item.surahNumber].orEmpty())
        view.findViewById<TextView>(R.id.bookmark_location).text=item.ayahNumber?.let {
            context.getString(R.string.bookmark_ayah_location,item.pageNumber,it)
        } ?: context.getString(R.string.ayah_page,item.pageNumber)
        view.findViewById<View>(R.id.bookmark_remove).apply {
            contentDescription = context.getString(R.string.bookmark_remove_named, names[item.surahNumber].orEmpty(), item.pageNumber)
            setOnClickListener { onRemove(item) }
        }
        return view
    }
}

class SearchResultAdapter(private val context: Context) : BaseAdapter() {
    private var items: List<com.ahmad.quran.data.Ayah> = emptyList()
    private var surahs: List<Surah> = emptyList()
    private var names: Map<Int,String> = emptyMap()
    fun submit(items: List<com.ahmad.quran.data.Ayah>, surahs: List<Surah>) {
        if(this.items === items && this.surahs === surahs) return
        this.items=items;this.surahs=surahs;names=surahs.associate { it.surahNumber to it.name }
        notifyDataSetChanged()
    }
    override fun getCount()=items.size
    override fun getItem(position: Int)=items[position]
    override fun getItemId(position: Int)=getItem(position).let { (it.surahNumber*1000+it.ayahNumber).toLong() }
    override fun hasStableIds()=true
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view=convertView ?: LayoutInflater.from(context).inflate(R.layout.row_search_result,parent,false)
        val ayah=getItem(position)
        view.findViewById<TextView>(R.id.search_result_reference).text=context.getString(R.string.quran_search_reference,names[ayah.surahNumber].orEmpty(),ayah.ayahNumber)
        // Native line clamping previews the original string; no normalized text or edited substring is displayed.
        view.findViewById<TextView>(R.id.search_result_text).text=ayah.text
        return view
    }
}

class ReadingHistoryAdapter(private val context: Context) : BaseAdapter() {
    private var items: List<ReadingHistoryEntry> = emptyList()
    private var names: Map<Int, String> = emptyMap()
    fun submit(items: List<ReadingHistoryEntry>, surahs: List<Surah>) {
        if (this.items == items && names.size == surahs.size) return
        this.items = items
        names = surahs.associate { it.surahNumber to it.name }
        notifyDataSetChanged()
    }
    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = items[position].pageNumber.toLong()
    override fun hasStableIds() = true
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_history, parent, false)
        val item = getItem(position)
        view.findViewById<TextView>(R.id.history_surah).text = context.getString(R.string.ayah_surah, names[item.surahNumber].orEmpty())
        view.findViewById<TextView>(R.id.history_location).text = item.ayahNumber?.let {
            context.getString(R.string.bookmark_ayah_location, item.pageNumber, it)
        } ?: context.getString(R.string.ayah_page, item.pageNumber)
        view.findViewById<TextView>(R.id.history_time).text = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, java.util.Locale.forLanguageTag("ar")
        ).format(java.util.Date(item.readAtMillis))
        return view
    }
}
