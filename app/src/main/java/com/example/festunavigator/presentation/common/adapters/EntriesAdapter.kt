package com.example.festunavigator.presentation.common.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.festunavigator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntriesAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<EntryItem, EntriesAdapter.ItemViewholder>(DiffCallback())  {

    private val adapterScope = CoroutineScope(Dispatchers.Default)
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
            textView.text = item.number
            descTextView.text = item.description

            setOnClickListener {
                onItemClick(item.number)
            }
        }
    }

    fun changeList(entries: List<EntryItem>){
        rawList = entries
        filter = ""
        submitList(rawList)
    }

    fun applyFilter(filter: String){
        this.filter = filter
        submitList(rawList
            .filter { it.number.startsWith(filter) }
            .sortedBy { it.number.length })
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