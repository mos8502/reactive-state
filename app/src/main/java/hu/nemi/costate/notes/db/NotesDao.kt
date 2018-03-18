package hu.nemi.costate.notes.db

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Entity(tableName = "notes")
data class NoteEntity(@PrimaryKey val id: String, val text: String)

@Dao
interface NotesDao {
    @Insert
    fun insert(note: NoteEntity)

    @Delete
    fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    fun delete(noteId: String)

    @Update
    fun update(note: NoteEntity)

    @Query("SELECT * FROM notes")
    fun getNotes(): List<NoteEntity>
}