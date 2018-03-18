package hu.nemi.costate.notes.impl

import hu.nemi.costate.notes.Editor
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.store.Middleware
import hu.nemi.store.coroutines.coroutineDispatcher
import hu.nemi.store.middlewareChain
import hu.nemi.store.store
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext

interface EditorService : Middleware<NotesImpl.State, NotesImpl.Message>

class NotesImpl(messageContext: CoroutineContext, stateContext: CoroutineContext, persistence: NotesPersistence, editorService: EditorService) : Notes {
    private val store = store(initialState = State(false, emptyList(), State.CreateState.Action), reducer = ::reduce)
    private val dispatcher = coroutineDispatcher(store = store,
            middlewareChain = middlewareChain(listOf(persistence, editorService)),
            messageContext = messageContext,
            stateContext = stateContext)
            .apply { dispatch(Message.GetNotes) }

    override fun onNotesChanged(block: (Notes.State) -> Unit): Closeable = dispatcher.subscribe { state ->
        state.entities.filter(::noteDeleting)
                .map { item ->
                    when (item) {
                        is Item.Display -> Notes.Item.Note(
                                entity = item.entity,
                                delete = { dispatcher.dispatch(Message.DeleteNote(item.entity.id)) },
                                edit = { dispatcher.dispatch(Message.Edit(item.entity.id)) })
                        is Item.Editing -> Notes.Item.Edit(
                                id = EditItemId(item.entity.id),
                                editor = item.editor)
                    }
                }
                .let {
                    val withCreate = when (state.createState) {
                        State.CreateState.None -> it
                        State.CreateState.Action -> it + Notes.Item.Create(id = CreateItemId, create = { dispatcher.dispatch(Message.OnCreate) })
                        is State.CreateState.Create -> it + Notes.Item.Edit(id = CreateItemId, editor = state.createState.editor)
                    }

                    block(Notes.State(withCreate))
                }
    }

    fun close() {
        dispatcher.close()
    }

    private fun noteDeleting(it: Item) = it !is Item.Display || (!it.flags.isDeleting)

    private fun reduce(state: State, message: Message): State = when (message) {
        Message.GetNotes -> onGetNotes(state)
        is Message.NotesChanged -> onNotesChanged(state, message)

        is Message.OnNoteDeleted -> onNoteDeleted(state, message.id)
        is Message.MarkDeleted -> onMarkDeleted(state, message.id)

        is Message.OnUpdating -> onUpdating(state, message.entity)
        is Message.Update -> onUpdated(state, message.entity)
        is Message.OnEditingStarted -> onEditingStarted(state, message.noteId, message.editor)
        is Message.CancelEdit -> onCancelEdit(state, message.entity)

        is Message.OnCreateStarted -> onCreateStarted(state, message.editor)
        Message.OnCreateFinished -> onCreateFinished(state)
        is Message.OnCreating -> onCreating(state, message.entity)
        is Message.OnCreated -> onCreated(state, message.entity)
        else -> state
    }

    private fun onCreated(state: State, entity: NoteEntity): State {
        val item = state[entity.id]
        return if (item is Item.Display) {
            val index = state.entities.indexOf(item)
            val items = state.entities.toMutableList().let {
                it[index] = item.copy(entity = entity, flags = item.flags.setCreating(false))
                it.toList()
            }
            state.copy(entities = items)
        } else {
            state
        }
    }

    private fun onCreating(state: State, entity: NoteEntity): State =
            state.copy(entities = state.entities + Item.Display(entity = entity, flags = CREATING))

    private fun onCreateFinished(state: State): State =
            state.copy(createState = State.CreateState.Action)

    private fun onCreateStarted(state: State, editor: Editor): State =
            state.copy(createState = State.CreateState.Create(editor))

    private fun onCancelEdit(state: State, entity: NoteEntity): State {
        val item = state[entity.id]
        return if (item != null && item is NotesImpl.Item.Editing) {
            val index = state.entities.indexOf(item)
            val items = state.entities.toMutableList().let {
                it[index] = NotesImpl.Item.Display(entity = entity, flags = 0x0)
                it.toList()
            }
            state.copy(entities = items)
        } else {
            state
        }
    }

    private fun onUpdated(state: State, entity: NoteEntity): State {
        val item = state[entity.id]
        return if (item != null && item is NotesImpl.Item.Display) {
            val index = state.entities.indexOf(item)
            val items = state.entities.toMutableList().let {
                it[index] = item.copy(entity = entity, flags = item.flags.setUpdating(false))
                it.toList()
            }
            state.copy(entities = items)
        } else {
            state
        }
    }

    private fun onUpdating(state: State, entity: NoteEntity): State {
        val item = state[entity.id]
        return if (item != null && item is NotesImpl.Item.Editing) {
            val index = state.entities.indexOf(item)
            val items = state.entities.toMutableList().let {
                it[index] = NotesImpl.Item.Display(entity = entity, flags = UPDATING)
                it.toList()
            }
            state.copy(entities = items)
        } else {
            state
        }
    }

    private fun onEditingStarted(state: State, noteId: String, editor: Editor): State {
        val item = state[noteId]
        return if (item != null) {
            val index = state.entities.indexOf(item)
            val items = state.entities.toMutableList().let {
                it[index] = Item.Editing(item.entity, editor)
                it.toList()
            }
            state.copy(entities = items)
        } else {
            state
        }
    }

    private fun onMarkDeleted(state: State, noteId: String): State =
            state.copy(entities = state.entities.map {
                if (it is Item.Display && it.entity.id == noteId) it.copy(flags = it.flags.setDeleting(true))
                else it
            })

    private fun onNoteDeleted(state: State, noteId: String) =
            state.copy(entities = state.entities.filter { it is Item.Display && it.entity.id != noteId })


    private fun onNotesChanged(state: State, message: Message.NotesChanged): State =
            state.copy(loading = false, entities = message.notes.map {
                state[it.id]?.withEntity(it)
                        ?: Item.Display(entity = it, flags = 0x0)
            })

    private fun onGetNotes(state: State) =
            state.copy(loading = true)

    sealed class Message {
        object GetNotes : Message()
        data class NotesChanged(val notes: List<NoteEntity>) : Message()

        // edit
        data class Edit(val id: String) : Message()

        data class OnEditingStarted(val noteId: String, val editor: Editor) : Message()
        data class Update(val entity: NoteEntity) : Message()
        data class OnUpdating(val entity: NoteEntity) : Message()
        data class OnUpdated(val entity: NoteEntity) : Message()
        data class CancelEdit(val entity: NoteEntity) : Message()

        // delete
        data class DeleteNote(val id: String) : Message()

        data class MarkDeleted(val id: String) : Message()
        data class OnNoteDeleted(val id: String) : Message()

        // create
        object OnCreate : Message()

        object OnCreateFinished : Message()
        data class Create(val text: String) : Message()
        data class OnCreateStarted(val editor: Editor) : Message()
        data class OnCreating(val entity: NoteEntity) : Message()
        data class OnCreated(val entity: NoteEntity) : Message()
    }

    sealed class Item {

        abstract fun withEntity(entity: NoteEntity): Item
        abstract val entity: NoteEntity

        data class Display(override val entity: NoteEntity, val flags: Int) : Item() {
            override fun withEntity(entity: NoteEntity): Item = copy(entity = entity)
        }

        data class Editing(override val entity: NoteEntity, val editor: Editor) : Item() {
            override fun withEntity(entity: NoteEntity): Item = copy(entity = entity)
        }
    }

    data class State(val loading: Boolean, val entities: List<Item>, val createState: CreateState) {
        operator fun get(noteId: String): Item? = entities.firstOrNull { it.entity.id == noteId }
        sealed class CreateState {
            object Action : CreateState()
            object None : CreateState()
            data class Create(val editor: Editor) : CreateState()
        }
    }
}

val Int.isDeleting: Boolean
    get() = this and DELETING > 0

val Int.isCreating: Boolean
    get() = this and CREATING > 0

val Int.isUpdating: Boolean
    get() = this and UPDATING > 0


private const val DELETING = 0x1
private const val UPDATING = 0x2
private const val CREATING = 0x4

private object CreateItemId : Notes.Item.Id

private class EditItemId(val noteId: String) : Notes.Item.Id

private fun Int.setDeleting(isDeleting: Boolean): Int = setFlag(DELETING, isDeleting)

private fun Int.setCreating(isCreating: Boolean): Int = setFlag(CREATING, isCreating)

private fun Int.setUpdating(isUpdating: Boolean): Int = setFlag(UPDATING, isUpdating)

private fun Int.setFlag(flag: Int, isSet: Boolean): Int = if (isSet) or(flag) else xor(flag)

