package hu.nemi.costate.notes.impl.editor

import hu.nemi.costate.di.Background
import hu.nemi.costate.di.Main
import hu.nemi.costate.notes.Editor
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.impl.EditorService
import hu.nemi.costate.notes.impl.NotesImpl
import hu.nemi.costate.notes.impl.NotesImpl.Message
import hu.nemi.costate.notes.impl.NotesImpl.State
import hu.nemi.costate.notes.impl.isDeleting
import hu.nemi.store.*
import hu.nemi.store.Middleware.Chain.Next
import hu.nemi.store.coroutines.coroutineDispatcher
import java.io.Closeable
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class EditorServiceImpl @Inject constructor(@Background val messageContext: CoroutineContext, @Main val stateContext: CoroutineContext) : EditorService {
    override fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message, next: Next<State, Message>) {
        next.dispatch(dispatcher, state, message)
        when (message) {
            is Message.Edit -> onEdit(dispatcher, state, message, next)
            Message.OnCreate -> onCreate(dispatcher)
        }
    }

    override fun close() = Unit

    private fun onCreate(dispatcher: Dispatcher<Message>) {
        val editor = EditorImpl(
                handler = CreateHandler(notesDispatcher = dispatcher),
                initialText = "",
                messageContext = messageContext,
                stateContext = stateContext)
        dispatcher.dispatch(Message.OnCreateStarted(editor))
    }

    private fun onEdit(dispatcher: Dispatcher<Message>, state: State, message: Message.Edit, next: Next<State, Message>) {
        val item = state[message.id]
        if (item is NotesImpl.Item.Display && !item.flags.isDeleting) {
            next.dispatch(dispatcher, state, message)
            startEditing(dispatcher, item)
        }
    }

    private fun startEditing(dispatcher: Dispatcher<Message>, item: NotesImpl.Item.Display) {
        val editor = EditorImpl(
                handler = UpdateHandler(entity = item.entity,
                        notesDispatcher = dispatcher),
                initialText = item.entity.text,
                messageContext = messageContext,
                stateContext = stateContext)
        dispatcher.dispatch(Message.OnEditingStarted(noteId = item.entity.id, editor = editor))
    }
}

class UpdateHandler(private val entity: NoteEntity,
                    private val notesDispatcher: Dispatcher<NotesImpl.Message>) : Middleware<EditorImpl.State, EditorImpl.Message> {
    override fun dispatch(dispatcher: Dispatcher<EditorImpl.Message>, state: EditorImpl.State, message: EditorImpl.Message, next: Next<EditorImpl.State, EditorImpl.Message>) {
        when (message) {
            EditorImpl.Message.Submit -> {
                next.dispatch(dispatcher, state, message)
                dispatcher.dispatch(EditorImpl.Message.Reset)
                notesDispatcher.dispatch(NotesImpl.Message.Update(entity.copy(text = state.text)))
            }
            EditorImpl.Message.Close -> {
                next.dispatch(dispatcher, state, message)
                notesDispatcher.dispatch(NotesImpl.Message.CancelEdit(entity))
                dispatcher.close()
            }
        }
    }

    override fun close() = Unit
}

class CreateHandler(private val notesDispatcher: Dispatcher<Message>) : Middleware<EditorImpl.State, EditorImpl.Message> {
    override fun dispatch(dispatcher: Dispatcher<EditorImpl.Message>, state: EditorImpl.State, message: EditorImpl.Message, next: Next<EditorImpl.State, EditorImpl.Message>) {
        when (message) {
            EditorImpl.Message.Submit -> {
                next.dispatch(dispatcher, state, message)
                dispatcher.dispatch(EditorImpl.Message.Reset)
                notesDispatcher.dispatch(NotesImpl.Message.Create(state.text))
            }
            EditorImpl.Message.Close -> {
                next.dispatch(dispatcher, state, message)
                notesDispatcher.dispatch(NotesImpl.Message.OnCreateFinished)
                dispatcher.close()
            }
        }
    }

    override fun close() = Unit
}

class EditorImpl(handler: Middleware<EditorImpl.State, EditorImpl.Message>, initialText: String = "", messageContext: CoroutineContext, stateContext: CoroutineContext) : Editor {
    private val dispatcher = coroutineDispatcher(
            store = store(State(text = initialText, reset = true), ::reduce),
            middlewareChain = middlewareChain(listOf(handler)),
            messageContext = messageContext,
            stateContext = stateContext)

    override fun onStateChanged(block: (Editor.State) -> Unit): Closeable = dispatcher.subscribe(block)

    override fun setText(text: String) = dispatcher.dispatch(Message.SetText(text))

    override fun submit() = dispatcher.dispatch(Message.Submit)

    override fun close() = dispatcher.dispatch(Message.Close)

    private fun reduce(state: State, message: Message): State = when (message) {
        is Message.SetText -> state.copy(text = message.text, reset = false)
        Message.Reset -> state.copy(text = "", reset = true)
        Message.Submit -> state
        Message.Close -> state
    }

    sealed class Message {
        data class SetText(val text: String) : Message()
        object Close : Message()
        object Reset : Message()
        object Submit : Message()
    }

    data class State(override val text: String, override val reset: Boolean) : Editor.State
}