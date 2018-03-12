package hu.nemi.costate

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import hu.nemi.costate.model.Notes

class ViewModelFactory(private val notes: Notes) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            NotesViewModel::class.java -> NotesViewModelImpl(notes) as T
            else -> throw IllegalArgumentException("unsupported view model $modelClass")
        }
    }
}