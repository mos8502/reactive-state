package hu.nemi.costate.notes.usecases

import hu.nemi.costate.di.BackgroundDispatcher
import hu.nemi.costate.di.StoreDispatcher
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.State
import hu.nemi.store.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class LoadNotes @Inject constructor(private val dao: NotesDao,
                                    @BackgroundDispatcher private val backgroundDispatcher: CoroutineContext,
                                    @StoreDispatcher private val storeDispatcher: CoroutineContext) : NoteUseCase {

    override fun execute(store: Store<State, Action>) {
        launch(context = storeDispatcher) {
            store.dispatch(loadNotes)
        }
    }

    private val loadNotes = object : AsyncActionCreator<State, Action> {
        override fun invoke(state: State, dispatch: (ActionCreator<State, Action>) -> Unit) {
            if (!state.isLoading && (state.error != null || System.currentTimeMillis() - state.lastFetched > NOTES_TTL)) launch(context = storeDispatcher) {
                dispatch(object : ActionCreator<State, Action> {
                    override fun invoke(state: State): Action? = Action.LoadNotes
                })
                try {
                    val notes = notes.await()
                    dispatch(object : ActionCreator<State, Action> {
                        override fun invoke(state: State): Action? = Action.NotesLoaded(timestamp = System.currentTimeMillis(), notes = notes)
                    })
                } catch (error: Throwable) {
                    dispatch(object : ActionCreator<State, Action> {
                        override fun invoke(state: State): Action? = Action.FailedToLoadNotes(cause = error)
                    })
                }
            }
        }
    }

    private val notes: Deferred<List<NoteEntity>>
        get() = async(context = backgroundDispatcher) { dao.getNotes() }
}

private const val NOTES_TTL = 180000
