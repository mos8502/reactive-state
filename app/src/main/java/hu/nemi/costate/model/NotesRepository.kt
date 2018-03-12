package hu.nemi.costate.model

import hu.nemi.costate.model.db.NoteEntity
import hu.nemi.costate.model.db.NotesDao
import hu.nemi.costate.model.network.NotesClient
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.reactive.openSubscription

data class Note(val id: String, val text: String)

interface NotesRepository {
    suspend fun add(text: String): Note
    suspend fun delete(note: Note)
    suspend fun update(note: Note)

    val notes: ReceiveChannel<List<Note>>
}

class NotesRepositoryImpl(private val notesDao: NotesDao, private val notesClient: NotesClient) : NotesRepository {

    override suspend fun add(text: String): Note =
            notesClient.addNote(text).apply {
                notesDao.insert(toEntity())
            }

    override suspend fun delete(note: Note) {
        notesClient.deleteNote(note)
        notesDao.delete(note.toEntity())
    }

    override suspend fun update(note: Note) {
        notesClient.updateNote(note)
        notesDao.update(note.toEntity())
    }

    override val notes: ReceiveChannel<List<Note>> = notesDao.notes
            .openSubscription()
            .map { it.map(NoteEntity::toNote) }

}

private fun NoteEntity.toNote() = Note(id = id, text = text)
private fun Note.toEntity() = NoteEntity(id = id, text = text)

