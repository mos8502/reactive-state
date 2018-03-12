package hu.nemi.costate.model.db

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

    @Update
    fun update(note: NoteEntity)

    @get:Query("SELECT * FROM notes")
    val notes: Flowable<List<NoteEntity>>
}