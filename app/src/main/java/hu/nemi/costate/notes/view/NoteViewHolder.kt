package hu.nemi.costate.notes.view

import android.support.v7.widget.RecyclerView
import android.view.View
import hu.nemi.costate.notes.model.ListItem

interface ListItemView {
    val view: View
    fun bind(listItem: ListItem) = when (listItem) {
        is ListItem.NoteItem -> bind(listItem)
        is ListItem.EditorItem -> bind(listItem)
        is ListItem.AddItem -> bind(listItem)
    }

    fun bind(noteItem: ListItem.NoteItem) {
        throw UnsupportedOperationException()
    }

    fun bind(editorItem: ListItem.EditorItem) {
        throw UnsupportedOperationException()
    }

    fun bind(addItem: ListItem.AddItem) {
        throw UnsupportedOperationException()
    }

    fun unbind() {}
}

class NotesViewHolder(private val noteView: ListItemView) : RecyclerView.ViewHolder(noteView.view) {
    fun bind(noteItem: ListItem) {
        noteView.bind(noteItem)
    }

    fun unbind() {
        noteView.unbind()
    }
}