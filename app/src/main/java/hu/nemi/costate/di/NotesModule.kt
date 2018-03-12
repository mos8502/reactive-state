package hu.nemi.costate.di

import android.arch.lifecycle.ViewModelProvider
import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import hu.nemi.costate.ViewModelFactory
import hu.nemi.costate.model.Notes
import hu.nemi.costate.model.NotesImpl
import hu.nemi.costate.model.NotesRepositoryImpl
import hu.nemi.costate.model.NotesRepository
import hu.nemi.costate.model.db.NotesDao
import hu.nemi.costate.model.db.NotesDatabase
import hu.nemi.costate.model.network.DefaultNotesClient
import hu.nemi.costate.model.network.NotesClient
import javax.inject.Singleton

@Module
class NotesModule {

    @[Provides Singleton]
    fun notesDatabase(context: Context): NotesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java, "notes").build()

    @[Provides Singleton]
    fun provideNotesDao(db: NotesDatabase): NotesDao = db.notesDao()

    @[Provides Singleton]
    fun provideNotesRepository(dao: NotesDao, client: NotesClient): NotesRepository = NotesRepositoryImpl(dao, client)

    @[Provides Singleton]
    fun provideViewModelFactory(notes: Notes) : ViewModelProvider.Factory = ViewModelFactory(notes)

    @[Provides Singleton]
    fun provideNotes(repository: NotesRepository): Notes = NotesImpl(repository)

    @get:[Provides Singleton] val notesClient: NotesClient = DefaultNotesClient()

}