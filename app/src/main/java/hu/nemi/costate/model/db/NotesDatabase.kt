package hu.nemi.costate.model.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import hu.nemi.costate.model.db.NotesDatabase.Companion.VERSION

@Database(version = VERSION, entities = [NoteEntity::class])
abstract class NotesDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 1
    }

    abstract fun notesDao(): NotesDao
}