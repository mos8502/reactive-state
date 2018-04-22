package hu.nemi.costate.notes.usecases

import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.State
import hu.nemi.store.Store

interface NoteUseCase {
    fun execute(store: Store<State, Action>)
}