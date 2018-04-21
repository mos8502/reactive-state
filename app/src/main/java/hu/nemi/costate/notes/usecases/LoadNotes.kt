package hu.nemi.costate.notes.usecases

import hu.nemi.costate.di.BackgroundDispatcher
import hu.nemi.costate.di.StoreDispatcher
import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.State
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.store.AsyncActionCreator
import hu.nemi.store.Optional
import hu.nemi.store.Store
import hu.nemi.store.asOptional
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class LoadNotes @Inject constructor(private val dao: NotesDao,
                                    @BackgroundDispatcher private val backgroundDispatcher: CoroutineContext,
                                    @StoreDispatcher private val storeDispatcher: CoroutineContext) {

    fun execute(store: Store<State, Action>) {
        launch(context = storeDispatcher) {
            store.dispatch(newLoadNotesAction(store))
        }
    }

    private fun newLoadNotesAction(store: Store<State, Action>): AsyncActionCreator<State, Action, Unit> = object : AsyncActionCreator<State, Action, Unit> {
        override fun onCreated(state: State) {
            launch(context = storeDispatcher) {
                try {
                    store.dispatch(Action.NotesLoaded(notes = notes.await(), timestamp = System.currentTimeMillis()))
                } catch (error: Throwable) {
                    store.dispatch(Action.FailedToLoadNotes(cause = error))
                }
            }
        }


        override fun invoke(state: State): Optional<Action> =
                if (!state.isLoading && (state.error != null || System.currentTimeMillis() - state.lastFetched > NOTES_TTL)) Action.LoadNotes.asOptional()
                else Optional.Empty

        private val notes: Deferred<List<NoteEntity>>
            get() = async(context = backgroundDispatcher) { dao.getNotes() }
    }
}

private const val NOTES_TTL = 180000
