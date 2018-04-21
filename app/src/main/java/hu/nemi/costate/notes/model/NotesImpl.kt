package hu.nemi.costate.notes.model

import hu.nemi.costate.arch.BlockViewModel
import hu.nemi.costate.notes.usecases.LoadNotes
import hu.nemi.store.Store
import hu.nemi.store.Subscription
import javax.inject.Inject

class NotesViewModel @Inject constructor(private val notes: Notes) : BlockViewModel<ViewState, Notes>(notes), NotesApi by notes

class NotesImpl @Inject constructor(private val store: Store<State, Action>,
                                    private val loadNotes: LoadNotes) : Notes {
    override fun loadNotes() = loadNotes.execute(store)

    override fun onActive() = loadNotes()

    override fun subscribe(block: (ViewState) -> Unit): Subscription =
            store.subscribe { block.invoke(it.toViewState()) }

    private fun State.toViewState(): ViewState =
            ViewState(notes = entities.map { NoteItem(id = it.id, text = it.text) })
}