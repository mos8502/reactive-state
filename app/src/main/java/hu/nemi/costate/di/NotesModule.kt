package hu.nemi.costate.di

import android.arch.lifecycle.ViewModelProvider
import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.db.NotesDatabase
import hu.nemi.costate.notes.impl.EditorService
import hu.nemi.costate.notes.impl.NotesImpl
import hu.nemi.costate.notes.impl.NotesPersistence
import hu.nemi.costate.notes.impl.NotesViewModelImpl
import hu.nemi.costate.notes.impl.editor.EditorServiceImpl
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import javax.inject.Singleton

@Module
class NotesModule {

    @[Provides Singleton]
    fun notesDatabase(context: Context): NotesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java, "notes").build()

    @[Provides Singleton]
    fun provideNotesDao(db: NotesDatabase): NotesDao = db.notesDao()

    @[Provides Singleton]
    fun provideNotesPersistence(dao: NotesDao) = NotesPersistence(dao = dao, context = DefaultDispatcher)

    @[Provides Singleton]
    fun provideEditorService(): EditorService = EditorServiceImpl(messageContext = DefaultDispatcher, stateContext = UI)

    @[Provides Singleton]
    fun provideNotes(persistence: NotesPersistence, editorService: EditorService): ViewModelProvider.Factory =
            NotesViewModelImpl.Factory(NotesImpl(messageContext = DefaultDispatcher, stateContext = UI, persistence = persistence, editorService = editorService))
}