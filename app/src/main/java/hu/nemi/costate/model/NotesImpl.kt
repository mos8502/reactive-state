package hu.nemi.costate.model

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class NotesImpl(private val repository: NotesRepository) : Notes {
    private val nextOperationId = AtomicLong()
    private val actor = actor<List<Message>>(capacity = 1) {
        var state = State()
        while (isActive) {
            state = select {
                channel.onReceive { actions -> onActions(actions, state) }
                repository.notes.onReceive { notes -> onNotesChanged(state, notes) }
            }
            publishState(state)
        }
    }

    override fun create(text: String): Job = launch {
        val operation = Operation.Add(
                id = nextOperationId.incrementAndGet(),
                noteId = "local:${UUID.randomUUID()}",
                text = text)
        actor.send(operation.add())
        try {
            operation.copy(note = repository.add(text)).run {
                actor.send(replace(), execute())
            }
        } catch (error: Throwable) {
            actor.send(operation.remove())
            throw error
        }
    }

    override fun delete(note: Note): Job = launch {
        val operation = Operation.Delete(id = nextOperationId.incrementAndGet(), note = note)
        actor.send(operation.add())
        try {
            repository.delete(note)
            actor.send(operation.execute())
        } catch (error: Throwable) {
            actor.send(operation.remove())
            throw error
        }
    }

    override fun update(note: Note): Job = launch {
        val operation = Operation.Update(id = nextOperationId.incrementAndGet(), note = note)
        actor.send(operation.add())
        try {
            repository.update(note)
            actor.send(operation.execute())
        } catch (error: Throwable) {
            actor.send(operation.remove())
            throw error
        }
    }

    private fun onActions(messages: List<Message>, state: State): State {
        return messages.fold(state) { state, action ->
            when (action) {
                is Message.Add -> onAdd(state, action)
                is Message.Remove -> onRemove(state, action)
                is Message.Replace -> onReplace(state, action)
                is Message.Execute -> onExecute(state, action)
            }
        }
    }

    override val notes = Channel<List<Note>>()

    private fun onExecute(state: State, message: Message.Execute) =
            state.copy(notes = state.operations[message.operationId](state.notes), operations = state.operations.filter { it.id != message.operationId })

    private fun onReplace(state: State, message: Message.Replace) =
            state.copy(operations = state.operations.map { if (it.id == message.operation.id) message.operation else it })

    private fun onRemove(state: State, message: Message.Remove) =
            state.copy(operations = state.operations.filter { it.id == message.operationId })

    private fun onAdd(state: State, message: Message.Add) =
            state.copy(operations = state.operations + message.operation)

    private suspend fun publishState(state: State) {
        notes.send(state.operations.fold(state.notes) { notes, action -> action(notes) })
    }

    private fun onNotesChanged(state: State, notes: List<Note>) =
            if (state.operations.isEmpty()) state.copy(notes = notes) else state

    private suspend fun SendChannel<List<Message>>.send(message: Message) = send(listOf(message))

    private suspend fun SendChannel<List<Message>>.send(vararg messages: Message) = send(messages.toList())

    private operator fun List<Operation>.get(id: Long) = first { it.id == id }

    private data class State(val notes: List<Note> = emptyList(), val operations: List<Operation> = emptyList())

    private sealed class Message {
        data class Add(val operation: Operation) : Message()
        data class Remove(val operationId: Long) : Message()
        data class Replace(val operation: Operation) : Message()
        data class Execute(val operationId: Long) : Message()
    }

    private sealed class Operation : (List<Note>) -> List<Note> {
        abstract val id: Long

        fun add() = Message.Add(this)

        fun remove() = Message.Remove(this.id)

        fun replace() = Message.Replace(this)

        fun execute() = Message.Execute(this.id)

        data class Add(override val id: Long, val note: Note) : Operation() {
            constructor(id: Long, noteId: String, text: String) : this(id = id, note = Note(noteId, text))

            override fun invoke(notes: List<Note>): List<Note> = notes + note
        }

        data class Delete(override val id: Long, val note: Note) : Operation() {
            override fun invoke(notes: List<Note>): List<Note> = notes.filter { it.id != note.id }
        }

        data class Update(override val id: Long, val note: Note, val noteId: String = note.id) : Operation() {
            override fun invoke(notes: List<Note>): List<Note> = notes.map {
                if (it.id == note.id) note
                else it
            }
        }
    }
}