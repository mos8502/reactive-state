package hu.nemi.costate.notes.impl

import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.NotesViewModel
import javax.inject.Inject

class NotesViewModelImpl @Inject constructor(private val notes: NotesImpl) : NotesViewModel(), Notes by notes {

    override fun onCleared() {
        notes.close()
    }
}