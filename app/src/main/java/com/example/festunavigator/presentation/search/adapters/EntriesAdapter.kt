package com.example.festunavigator.presentation.search.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.festunavigator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class EntriesAdapter(
    private val onItemClick: (String) -> Unit,
    private val onEmptyList: () -> Unit,
    private val onNotEmptyList: () -> Unit,
) : ListAdapter<EntryItem, EntriesAdapter.ItemViewholder>(DiffCallback())  {

    private var recordsList = listOf<EntryItem>()
    private var rawList = listOf<EntryItem>()
    private var filter: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewholder {
        return ItemViewholder(
            onItemClick,
            LayoutInflater.from(parent.context)
                .inflate(R.layout.entry_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: EntriesAdapter.ItemViewholder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewholder(private val onItemClick: (String) -> Unit, itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: EntryItem) = with(itemView) {
            val textView: TextView = this.findViewById(R.id.entry_number)
            val descTextView: TextView = this.findViewById(R.id.description_text)
            val historyImage: ImageView = this.findViewById(R.id.image_history)
            textView.text = item.number
            descTextView.text = item.description
            historyImage.isVisible = item.history

            setOnClickListener {
                onItemClick(item.number)
            }
        }
    }

    fun changeList(entries: List<EntryItem>){
        rawList = entries
        applyFilter(filter)
    }

    fun changeHistory(records: List<EntryItem>){
        recordsList = records
        applyFilter(filter)
    }

    fun applyFilter(filter: String){
        this.filter = filter
        if (filter == "") {
            submitList(recordsList)
            if (recordsList.isEmpty()) onEmptyList() else onNotEmptyList()
        }
        else {
            submitList((rawList)
                .filter { it.number.startsWith(filter) }
            )
            if (rawList.isEmpty()) onEmptyList() else onNotEmptyList()
        }
    }
}

class DiffCallback : DiffUtil.ItemCallback<EntryItem>() {
    override fun areItemsTheSame(oldItem: EntryItem, newItem: EntryItem): Boolean {
        return oldItem.number == newItem.number
    }

    override fun areContentsTheSame(oldItem: EntryItem, newItem: EntryItem): Boolean {
        return oldItem == newItem
    }
}