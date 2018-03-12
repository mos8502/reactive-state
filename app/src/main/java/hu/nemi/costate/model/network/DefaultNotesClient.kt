package hu.nemi.costate.model.network

import hu.nemi.costate.model.Note
import kotlinx.coroutines.experimental.delay
import java.util.*

class DefaultNotesClient : NotesClient {
    override suspend fun addNote(text: String): Note {
        delay(3000L)
        return Note(id = UUID.randomUUID().toString(), text = text)
    }

    override suspend fun deleteNote(note: Note) = delay(3000L)

    override suspend fun updateNote(note: Note) = delay(3000L)
}