package hu.nemi.costate.di

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.db.NotesDatabase
import javax.inject.Singleton

@Module
class DatabaseModule {
    @[Provides Singleton]
    fun notesDatabase(context: Context): NotesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java, "notes").build()

    @[Provides Singleton]
    fun provideNotesDao(db: NotesDatabase): NotesDao = db.notesDao()
}