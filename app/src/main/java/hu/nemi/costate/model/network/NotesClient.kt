package hu.nemi.costate.model.network

import hu.nemi.costate.model.Note

interface NotesClient {
    suspend fun addNote(text: String): Note

    suspend fun deleteNote(note: Note)

    suspend fun updateNote(note: Note)

}