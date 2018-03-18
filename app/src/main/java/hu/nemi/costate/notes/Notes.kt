package hu.nemi.costate.notes

import hu.nemi.costate.notes.db.NoteEntity
import java.io.Closeable

interface Notes {
    sealed class Item {
        abstract val id: Id

        class Note(val entity: NoteEntity, val delete: () -> Unit, val edit: () -> Unit) : Item() {
            override val id = Id(entity.id)

            data class Id(val localId: String) : Notes.Item.Id
        }

        class Edit(override val id: Id, val editor: Editor) : Item()

        class Create(override val id: Id, val create: () -> Unit) : Item()

        interface Id
    }

    fun onNotesChanged(block: (State) -> Unit): Closeable

    data class State(val notes: List<Item>)
}