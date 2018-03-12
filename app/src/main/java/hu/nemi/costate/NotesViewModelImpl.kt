package hu.nemi.costate

import android.arch.lifecycle.MutableLiveData
import hu.nemi.costate.model.Note
import hu.nemi.costate.model.Notes
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import kotlin.coroutines.experimental.CoroutineContext

class NotesViewModelImpl(private val notes: Notes) : NotesViewModel() {
    private var _state = State(notes = emptyList(), operation = Operation.Add(::create))
    private val actor = actor<Action> {
        while (isActive) {
            select<Unit> {
                notes.notes.onReceive {
                    _state = _state.copy(notes = it)
                    publishState()
                }
                channel.onReceive { action ->
                    when (action) {
                        is Action.Delete -> onDelete(action)
                        is Action.Edit -> onEdit(action)
                        Action.Create -> onCreate()
                        is Action.Add -> onAdd(action)
                        is Action.Update -> onUpdate(action)
                        Action.CancelOperation -> onCancelOperation()
                    }
                }
            }
        }
    }
    override val state = MutableLiveData<NotesViewModel.State>()

    override fun delete(note: Note) {
        launch { actor.send(Action.Delete(note)) }
    }

    override fun edit(note: Note) {
        launch { actor.send(Action.Edit(note)) }
    }

    override fun create() {
        launch { actor.send(Action.Create) }
    }

    private fun publishState() {
        val items = _state.notes.mapTo(mutableListOf<LineItem>()) { note ->
            LineItem.DisplayItem(id = ItemId.DisplayItemId(note.id), note = note) {
                edit(note)
            }
        }
        state.postValue(State(_state.operation(items)))
    }

    private fun onDelete(action: Action.Delete) {
        notes.delete(action.note)
    }

    private fun ActorScope<Action>.onEdit(action: Action.Edit) {
        if (_state.operation is Operation.Add) {
            val editor = EditorImpl(initialText = action.note.text, context = coroutineContext)
            _state = _state.copy(operation = Operation.Edit(id = ItemId.DisplayItemId(action.note.id), editor = editor))
            launch {
                val event = editor.events.receive()
                when (event) {
                    is EditorImpl.Event.OnSubmit -> channel.send(Action.Update(action.note.copy(text = event.text)))
                    EditorImpl.Event.OnFinish -> channel.send(Action.CancelOperation)
                }
            }

            publishState()
        }
    }

    private fun ActorScope<Action>.onCreate() {
        if (_state.operation is Operation.Add) {
            val editor = EditorImpl(context = coroutineContext)
            _state = _state.copy(operation = Operation.Create(editor))
            launch {
                editor.events.takeWhile { it is EditorImpl.Event.OnSubmit }
                        .map { it as EditorImpl.Event.OnSubmit }
                        .map { it.text }
                        .consumeEach { text -> channel.send(Action.Add(text)) }
                channel.send(Action.CancelOperation)
            }

            publishState()
        }
    }

    private fun onAdd(action: Action.Add) {
        notes.create(action.text)
    }

    private fun onUpdate(action: Action.Update) {
        notes.update(action.note)
        onCancelOperation()
    }

    private fun onCancelOperation() {
        _state.operation.cancel()
        _state = _state.copy(operation = Operation.Add(::create))
        publishState()
    }

    private data class State(val notes: List<Note>, val operation: Operation)

    private sealed class Action {
        data class Delete(val note: Note) : Action()
        data class Edit(val note: Note) : Action()
        data class Add(val text: String) : Action()
        data class Update(val note: Note) : Action()
        object CancelOperation : Action()
        object Create : Action()
    }

    private sealed class Operation : (List<LineItem>) -> List<LineItem> {
        abstract fun cancel()

        data class Add(val onCreate:() -> Unit) : Operation() {
            override fun invoke(lineItems: List<LineItem>) = lineItems + LineItem.AddItem(ItemId.AddItemId, onCreate)
            override fun cancel() = Unit
        }

        data class Create(val editor: EditorImpl) : Operation() {
            override fun invoke(lineItems: List<LineItem>) = lineItems + LineItem.EditItem(ItemId.CreateItemId, editor)
            override fun cancel() {
                editor.cancel()
            }
        }

        data class Edit(val id: ItemId.DisplayItemId, val editor: EditorImpl) : Operation() {
            override fun invoke(lineItems: List<LineItem>) = lineItems.map {
                if (it.id == id) LineItem.EditItem(ItemId.EditItemId(id.noteId), editor)
                else it
            }

            override fun cancel() {
                editor.cancel()
            }
        }
    }
}

private class EditorImpl(private val initialText: String = "", val context: CoroutineContext) : Editor {
    private val _state = ConflatedBroadcastChannel<State>()
    private val _events = Channel<Event>(capacity = Channel.UNLIMITED)
    private val actor = actor<Message> {
        var state = State(text = initialText, reset = true)
        _state.send(state)
        while (isActive && !state.isFinished) {
            state = select {
                channel.onReceive { event ->
                    when (event) {
                        is Message.OnSetText -> state.copy(text = event.text, reset = false)
                        Message.OnSubmit -> {
                            _events.send(Event.OnSubmit(text = state.text))
                            state.copy(text = "", reset = true)
                        }
                        Message.OnFinish -> {
                            _events.send(Event.OnFinish)
                            state.copy(isFinished = true)
                        }
                    }
                }
            }
            if (!state.isFinished) _state.send(state)
        }
    }
    val events: ReceiveChannel<Event> = _events
    override val state: ReceiveChannel<Editor.State>
        get() = _state.openSubscription()


    override fun setText(text: String) {
        launch { actor.send(Message.OnSetText(text)) }
    }

    override fun submit() {
        launch { actor.send(Message.OnSubmit) }
    }

    override fun finish() {
        launch { actor.send(Message.OnFinish) }
    }

    fun cancel() = actor.close(CancellationException())

    sealed class Event {
        data class OnSubmit(val text: String) : Event()
        object OnFinish : Event()
    }

    private data class State(override val text: String,
                             override val reset: Boolean = false,
                             val isFinished: Boolean = false) : Editor.State

    private sealed class Message {
        data class OnSetText(val text: String) : Message()
        object OnSubmit : Message()
        object OnFinish : Message()
    }

}

private sealed class ItemId : NotesViewModel.LineItem.Id {
    data class DisplayItemId(val noteId: String) : ItemId()
    data class EditItemId(val noteId: String) : ItemId()
    object CreateItemId : ItemId()
    object AddItemId: ItemId()
}