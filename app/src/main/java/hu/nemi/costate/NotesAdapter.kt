package hu.nemi.costate

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlin.properties.Delegates

class NotesAdapter : RecyclerView.Adapter<NoteViewHolder>() {
    var notes: List<NotesViewModel.LineItem> by Delegates.observable(emptyList()) { _, oldNotes, newNotes ->
        DiffUtil.calculateDiff(NotesDiffCallback(oldNotes, newNotes)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return when (viewType) {
            VIEW_TYPE_NOTE -> NoteDisplayItemView(parent.context).wrap()
            VIEW_TYP_EDIT -> NoteEditItemView(parent.context).wrap()
            VIEW_TYPE_ADD -> AddItemView(parent.context).wrap()
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount() = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val lineItem = notes[position]
        when(lineItem) {
            is NotesViewModel.LineItem.DisplayItem -> holder.bind(lineItem)
            is NotesViewModel.LineItem.EditItem -> holder.bind(lineItem)
            is NotesViewModel.LineItem.AddItem -> holder.bind(lineItem)
        }
    }

    override fun getItemViewType(position: Int) = when (notes[position]) {
        is NotesViewModel.LineItem.DisplayItem -> VIEW_TYPE_NOTE
        is NotesViewModel.LineItem.EditItem -> VIEW_TYP_EDIT
        is NotesViewModel.LineItem.AddItem -> VIEW_TYPE_ADD
    }
}

class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

private fun <T: NotesViewModel.LineItem, V> V.wrap() : NoteViewHolder where V : View, V: LineItemView<T> {
    return NoteViewHolder(this)
}

private fun <T: NotesViewModel.LineItem> NoteViewHolder.bind(lineItem: T) {
    (itemView as LineItemView<T>).bind(lineItem)
}

private class NotesDiffCallback(val oldItems: List<NotesViewModel.LineItem>, val newItems: List<NotesViewModel.LineItem>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition].id == newItems[newItemPosition].id

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]
}

private const val VIEW_TYPE_NOTE = 0x1
private const val VIEW_TYP_EDIT = 0x2
private const val VIEW_TYPE_ADD = 0x3