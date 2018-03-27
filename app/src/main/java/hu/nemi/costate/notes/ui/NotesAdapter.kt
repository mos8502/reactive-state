package hu.nemi.costate.notes.ui

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import hu.nemi.costate.notes.Notes

class NotesAdapter : ListAdapter<Notes.Item, NotesViewHolder>(NotesDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder =
            when (viewType) {
                VIEW_TYPE_NOTE -> NoteItemView(parent.context).wrap()
                VIEW_TYPE_EDIT -> EditItemView(parent.context).wrap()
                VIEW_TYPE_CREATE -> CreateItemView(parent.context).wrap()
                else -> throw IllegalArgumentException("unsupported view type: $viewType")
            }


    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Notes.Item.Note -> VIEW_TYPE_NOTE
        is Notes.Item.Edit -> VIEW_TYPE_EDIT
        is Notes.Item.Create -> VIEW_TYPE_CREATE
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) =
            holder.bind(getItem(position))

    public override fun getItem(position: Int): Notes.Item {
        return super.getItem(position)
    }

    private companion object {
        const val VIEW_TYPE_NOTE = 0x1
        const val VIEW_TYPE_EDIT = 0x2
        const val VIEW_TYPE_CREATE = 0x3
    }

    fun <M, V> V.wrap(): NotesViewHolder where V : BindableView<M>, V : View, M : Notes.Item {
        return NotesViewHolder(this)
    }
}

class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    inline fun <reified T : Notes.Item> bind(model: T) {
        (itemView as BindableView<T>).bind(model)
    }
}

private object NotesDiffCallback: DiffUtil.ItemCallback<Notes.Item>() {
    override fun areItemsTheSame(oldItem: Notes.Item, newItem: Notes.Item): Boolean =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Notes.Item, newItem: Notes.Item): Boolean =
            oldItem == newItem
}