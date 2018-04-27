package hu.nemi.costate.notes.usecases

import hu.nemi.costate.di.BackgroundDispatcher
import hu.nemi.costate.di.StoreDispatcher
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.State
import hu.nemi.store.ActionCreator
import hu.nemi.store.AsyncActionCreator
import hu.nemi.store.Store
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class SaveEdit @Inject constructor(private val dao: NotesDao,
                                   @StoreDispatcher private val storeDispatcher: CoroutineContext,
                                   @BackgroundDispatcher private val backgroundDispatcher: CoroutineContext) : NoteUseCase {

    override fun execute(store: Store<State, Action>) {
        store.dispatch(updateNote)
    }

    private val updateNote = object : AsyncActionCreator<State, Action> {
        override fun invoke(state: State, dispatch: (ActionCreator<State, Action>) -> Unit) {
            if (state.editor != null) launch(context = storeDispatcher) {
                dispatch(object : ActionCreator<State, Action> {
                    override fun invoke(state: State): Action? = Action.SaveEdit
                })

                try {
                    val entity = NoteEntity(id = state.editor.noteId!!, text = state.editor.text)
                    updateEntity(entity).join()
                    dispatch(object : ActionCreator<State, Action> {
                        override fun invoke(state: State): Action? = Action.OnSaveSuccessful(entity = entity)
                    })
                } catch (error: Throwable) {
                    dispatch(object : ActionCreator<State, Action> {
                        override fun invoke(state: State): Action? = Action.OnSaveFailed(error = error)
                    })
                }
            }
        }
    }

    private fun updateEntity(entity: NoteEntity) = launch(context = backgroundDispatcher) {
        dao.update(entity)
    }
}
