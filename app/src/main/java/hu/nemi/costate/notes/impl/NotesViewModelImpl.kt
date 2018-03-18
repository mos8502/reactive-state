package hu.nemi.costate.notes.impl

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.NotesViewModel

class NotesViewModelImpl(private val notes: NotesImpl) : NotesViewModel(), Notes by notes {
    override fun onCleared() {
        notes.close()
    }

    class Factory(private val notes: NotesImpl) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = when (modelClass) {
            NotesViewModel::class.java -> NotesViewModelImpl(notes)
            else -> throw IllegalArgumentException("unsupported view model: $modelClass")
        } as T
    }
}