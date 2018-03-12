package hu.nemi.costate

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import hu.nemi.costate.model.Note
import kotlinx.coroutines.experimental.channels.ReceiveChannel

abstract class NotesViewModel : ViewModel() {
    abstract val state: LiveData<State>

    data class State(val notes: List<LineItem>)

    sealed class LineItem {
        abstract val id: Id

        data class DisplayItem(override val id: Id, val note: Note, val edit: () -> Unit) : LineItem()
        data class EditItem(override val id: Id, val editor: Editor) : LineItem()
        data class AddItem(override val id: Id, val onCreate: () -> Unit) : LineItem()

        interface Id
    }

    abstract fun delete(note: Note)
    abstract fun edit(note: Note)
    abstract fun create()
}

interface Editor {
    val state: ReceiveChannel<State>

    fun setText(text: String)
    fun submit()
    fun finish()

    interface State {
        val text: String
        val reset: Boolean
    }
}
