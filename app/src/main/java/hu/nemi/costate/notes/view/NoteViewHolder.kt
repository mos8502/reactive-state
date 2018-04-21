package hu.nemi.costate.notes.view

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.nemi.costate.R
import hu.nemi.costate.notes.model.NoteItem
import kotlinx.android.synthetic.main.view_notes.view.*

class NoteView private constructor(val view: View) {
    private val note by lazy { view.text }

    fun bind(noteItem: NoteItem) {
        note.text = noteItem.text
    }

    companion object {
        operator fun invoke(parent: ViewGroup): NoteView =
                NoteView(LayoutInflater.from(parent.context).inflate(R.layout.view_notes, parent, false))
    }
}

class NotesViewHolder(private val noteView: NoteView) : RecyclerView.ViewHolder(noteView.view) {
    fun bind(noteItem: NoteItem) {
        noteView.bind(noteItem)
    }
}