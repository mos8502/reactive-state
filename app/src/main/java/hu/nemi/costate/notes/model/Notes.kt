package hu.nemi.costate.notes.model

import hu.nemi.costate.arch.Block
import hu.nemi.costate.arch.BlockLifecycleCallback

interface NotesApi : Block<ViewState> {
    fun loadNotes()
}

interface Notes : NotesApi, BlockLifecycleCallback

data class ViewState(val notes: List<NoteItem>)

data class NoteItem(val id: String, val text: String)
