package hu.nemi.costate.notes.impl

import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.db.NotesDao
import hu.nemi.costate.notes.impl.NotesImpl.Message
import hu.nemi.costate.notes.impl.NotesImpl.State
import hu.nemi.store.Dispatcher
import hu.nemi.store.MessageSink
import hu.nemi.store.Middleware
import hu.nemi.store.Middleware.Chain.Next
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

class NotesPersistence(private val dao: NotesDao, private val context: CoroutineContext) : Middleware<State, Message> {
    private val job = Job()

    override fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message, next: Next<State, Message>) {
        next.dispatch(dispatcher, state, message)
        when (message) {
            Message.GetNotes -> onGetNotes(dispatcher)
            is Message.DeleteNote -> onDeleteNote(state, message, dispatcher)
            is Message.Update -> onUpdate(dispatcher, message.entity)
            is Message.Create -> onCreate(dispatcher, message.text)
        }
    }

    private fun onCreate(dispatcher: Dispatcher<Message>, text: String) {
        launch(context = context) {
            val entity = NoteEntity(id = UUID.randomUUID().toString(), text = text)
            dispatcher.dispatch(Message.OnCreating(entity))
            dao.insert(entity)
            dispatcher.dispatch(Message.OnCreated(entity))
        }
    }

    private fun onUpdate(dispatcher: MessageSink<Message>, entity: NoteEntity) {
        launch(context = context) {
            dispatcher.dispatch(NotesImpl.Message.OnUpdating(entity))
            dao.update(entity)
            dispatcher.dispatch(NotesImpl.Message.OnUpdated(entity))
        }
    }

    private fun onDeleteNote(state: State, message: Message.DeleteNote, dispatcher: MessageSink<Message>) {
        val item = state[message.id]
        if (item != null && item is NotesImpl.Item.Display && !item.flags.isDeleting) {
            dispatcher.dispatch(Message.MarkDeleted(message.id))
            launch {
                dao.delete(message.id)
                dispatcher.dispatch(Message.OnNoteDeleted(message.id))
            }
        }
    }

    private fun onGetNotes(dispatcher: MessageSink<Message>) {
        launch(context = context, parent = job) {
            dispatcher.dispatch(Message.NotesChanged(dao.getNotes()))
        }
    }

    override fun close() {
        job.cancel()
    }
}