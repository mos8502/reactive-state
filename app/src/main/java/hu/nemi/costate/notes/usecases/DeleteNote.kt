package hu.nemi.costate.notes.usecases

import hu.nemi.costate.di.BackgroundDispatcher
import hu.nemi.costate.di.StoreDispatcher
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.model.Action
import hu.nemi.costate.notes.model.State
import hu.nemi.store.Store
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class DeleteNote(private val dao: NotesDao,
                 @StoreDispatcher private val storeDispatcher: CoroutineContext,
                 @BackgroundDispatcher private val backgroundDispatcher: CoroutineContext,
                 private val noteId: String) : NoteUseCase {

    override fun execute(store: Store<State, Action>) {
        launch(context = storeDispatcher) {
            try {
                deleteNote().join()
                store.dispatch(Action.DeleteNote(noteId))
            } catch (ignore: Throwable) {

            }
        }
    }

    private fun deleteNote(): Job = launch(context = backgroundDispatcher) {
        dao.delete(noteId)
    }

    interface Factory {
        fun create(noteId: String): DeleteNote
    }
}

class DeleteNoteFactory @Inject constructor(private val dao: NotesDao,
                        @StoreDispatcher private val storeDispatcher: CoroutineContext,
                        @BackgroundDispatcher private val backgroundDispatcher: CoroutineContext) : DeleteNote.Factory {
    override fun create(noteId: String): DeleteNote =
            DeleteNote(dao = dao, storeDispatcher = storeDispatcher, backgroundDispatcher = backgroundDispatcher, noteId = noteId)
}