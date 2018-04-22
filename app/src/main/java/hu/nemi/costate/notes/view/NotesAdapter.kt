package hu.nemi.costate.notes.view

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import hu.nemi.costate.notes.model.ListItem

class NoteDiffCallback : DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean =
            oldItem == newItem
}

class NotesAdapter(diff: DiffUtil.ItemCallback<ListItem> = NoteDiffCallback()) : ListAdapter<ListItem, NotesViewHolder>(diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder =
            NotesViewHolder(when (viewType) {
                VIEW_TYPE_NOTE -> NoteView(parent)
                VIEW_TYPE_EDITOR -> EditorView(parent)
                VIEW_TYPE_ADD -> AddView(parent)
                else -> throw IllegalArgumentException()
            })

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) =
            holder.bind(getItem(position))

    public override fun getItem(position: Int): ListItem {
        return super.getItem(position)
    }

    override fun getItemViewType(position: Int): Int = getItem(position).itemViewType

    override fun onViewRecycled(holder: NotesViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }
}

private val ListItem.id: Any
    get() = when (this) {
        is ListItem.NoteItem -> ListItem.NoteItem::class to id
        is ListItem.EditorItem -> ListItem.EditorItem::class to id
        is ListItem.AddItem -> ListItem.AddItem::class
    }

private val ListItem.itemViewType: Int
    get() = when(this) {
        is ListItem.NoteItem -> VIEW_TYPE_NOTE
        is ListItem.EditorItem -> VIEW_TYPE_EDITOR
        is ListItem.AddItem -> VIEW_TYPE_ADD
    }

private const val VIEW_TYPE_NOTE = 0x0
private const val VIEW_TYPE_EDITOR = VIEW_TYPE_NOTE + 1
private const val VIEW_TYPE_ADD = VIEW_TYPE_EDITOR + 1
