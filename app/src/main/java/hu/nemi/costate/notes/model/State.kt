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
         * Last error
         */
        val error: Throwable?
)

sealed class Action {
    object LoadNotes: Action()
    data class NotesLoaded(val timestamp: Long, val notes: List<NoteEntity>): Action()
    data class FailedToLoadNotes(val cause: Throwable): Action()
}

inline fun <reified A: Action> reducer(crossinline block: (State, A) -> State): (State, Action) -> State = { state, action ->
    when(action) {
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

val notesReducer = compose(loadNotes, notesLoaded, failedToLoadNotes)

val INITIAL_STATE = State(entities = emptyList(), lastFetched = -1, isLoading = false, error = null)

val store = Store(initialState = INITIAL_STATE, reducer = notesReducer, middlewares = emptyList())

