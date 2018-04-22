package hu.nemi.costate.notes.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.nemi.costate.R
import hu.nemi.costate.notes.model.ListItem
import kotlinx.android.synthetic.main.view_notes.view.*

class NoteView private constructor(override val view: View) : ListItemView {
    private val note = view.text

    override fun bind(noteItem: ListItem.NoteItem) {
        note.text = noteItem.text
        view.setOnClickListener {
            noteItem.onClicked()
        }
    }

    companion object {
        operator fun invoke(parent: ViewGroup): NoteView =
                NoteView(LayoutInflater.from(parent.context).inflate(R.layout.view_notes, parent, false))
    }
}