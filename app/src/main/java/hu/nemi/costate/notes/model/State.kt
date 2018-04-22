package hu.nemi.costate.notes.model

import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.store.Store
import hu.nemi.store.compose

data class State(
        /**
         * List of notes
         */
        val entities: List<NoteEntity>,
        /**
         * Last time notes has been fetched
         */
        val lastFetched: Long,
        /**
         * Indicates whether notes are being loaded
         */
        val isLoading: Boolean,

        /**
         * Note editors
         */
        val editor: Editor?,

        /**
         * Last error
         */
        val error: Throwable?
)

/**
 * Note editors state
 */
data class Editor(
        /**
         * Current text in the editor
         */
        val text: String,
        /**
         * Error
         */
        val error: Throwable?,
        /**
         * Identifier of the note this editor is attached to
         */
        val noteId: String?,

        /**
         * Indicates whether the editor content is being saved
         */
        val isSaving: Boolean)

sealed class Action {
    object LoadNotes : Action()
    data class NotesLoaded(val timestamp: Long, val notes: List<NoteEntity>) : Action()
    data class FailedToLoadNotes(val cause: Throwable) : Action()

    data class EditNote(val noteId: String, val editorId: String) : Action()
    object CancelEdit : Action()
    data class UpdateEditorText(val text: String) : Action()
    object SaveEdit : Action()
    data class OnSaveSuccessful(val entity: NoteEntity) : Action()
    data class OnSaveFailed(val error: Throwable) : Action()

    object NewNote : Action()
    data class OnNoteAdded(val entity: NoteEntity): Action()
}

inline fun <reified A : Action> reducer(crossinline block: (State, A) -> State): (State, Action) -> State = { state, action ->
    when (action) {
        is A -> block(state, action)
        else -> state
    }
}

val loadNotes = reducer<Action.LoadNotes> { state, _ ->
    state.copy(isLoading = true)
}

val notesLoaded = reducer<Action.NotesLoaded> { state, action ->
    state.copy(isLoading = false, entities = action.notes, lastFetched = action.timestamp)
}

val failedToLoadNotes = reducer<Action.FailedToLoadNotes> { state, action ->
    state.copy(isLoading = false, error = action.cause)
}

val editNote = reducer<Action.EditNote> { state, action ->
    state.copy(editor = Editor(noteId = action.noteId,
            text = state.entities.first { it.id == action.noteId }.text,
            error = null,
            isSaving = false))
}

val cancelEdit = reducer<Action.CancelEdit> { state, action ->
    state.copy(editor = null)
}

val updateEditorText = reducer<Action.UpdateEditorText> { state, action ->
    state.copy(editor = state.editor?.copy(text = action.text))
}

val saveEdit = reducer<Action.SaveEdit> { state, action ->
    state.copy(editor = state.editor?.copy(isSaving = true))
}

val onSaveSuccessful = reducer<Action.OnSaveSuccessful> { state, action ->
    state.copy(
            editor = null,
            entities = state.entities.map {
                if (it.id == action.entity.id) action.entity
                else it
            }
    )
}

val onSaveFailed = reducer<Action.OnSaveFailed> { state, action ->
    state.copy(editor = state.editor?.copy(isSaving = false, error = action.error))
}

val newNote = reducer<Action.NewNote> { state, action ->
    state.copy(editor = Editor(text = "", isSaving = false, error = null, noteId = null))
}

val onNoteAdded = reducer<Action.OnNoteAdded> { state, action ->
    state.copy(entities = state.entities + action.entity, editor = null)
}

val notesReducer = compose(loadNotes,
        notesLoaded,
        failedToLoadNotes,
        editNote,
        cancelEdit,
        updateEditorText,
        saveEdit,
        onSaveSuccessful,
        onSaveFailed,
        newNote,
        onNoteAdded)

val INITIAL_STATE = State(entities = emptyList(), lastFetched = -1, isLoading = false, error = null, editor = null)

val store = Store(initialState = INITIAL_STATE, reducer = notesReducer, middlewares = emptyList())

