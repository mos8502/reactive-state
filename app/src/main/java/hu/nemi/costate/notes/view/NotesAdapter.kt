package hu.nemi.costate.notes.view

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import hu.nemi.costate.notes.model.NoteItem

class NoteDiffCallback : DiffUtil.ItemCallback<NoteItem>() {
    override fun areItemsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean =
            oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean =
            oldItem == newItem
}

class NotesAdapter(diff: DiffUtil.ItemCallback<NoteItem> = NoteDiffCallback()) : ListAdapter<NoteItem, NotesViewHolder>(diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder =
            NotesViewHolder(NoteView(parent))

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) =
            holder.bind(getItem(position))
}