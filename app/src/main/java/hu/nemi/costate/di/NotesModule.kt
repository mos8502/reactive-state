package hu.nemi.costate.di

import dagger.Binds
import dagger.Module
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.impl.EditorService
import hu.nemi.costate.notes.impl.NotesImpl
import hu.nemi.costate.notes.impl.editor.EditorServiceImpl

@Module
abstract class NotesModule {

    @Binds
    abstract fun bindNotes(notes: NotesImpl): Notes

    @Binds
    abstract fun bindEditorService(editorService: EditorServiceImpl): EditorService
}