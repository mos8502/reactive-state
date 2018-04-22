package hu.nemi.costate.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import hu.nemi.costate.notes.model.NotesImpl
import hu.nemi.costate.notes.model.State
import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.Notes
import hu.nemi.costate.notes.model.store
import hu.nemi.costate.notes.usecases.DeleteNote
import hu.nemi.costate.notes.usecases.DeleteNoteFactory
import hu.nemi.store.Store

@Module
abstract class NotesModule {

    @Binds
    abstract fun bindNotes(notes: NotesImpl): Notes

    @Binds
    abstract fun bindDeleteNoteFactory(factory: DeleteNoteFactory): DeleteNote.Factory

    @Module
    companion object {
        @Provides
        @JvmStatic fun provideAppStore(): Store<State, Action> = store
    }
}