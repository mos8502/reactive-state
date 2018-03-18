package hu.nemi.costate.notes

import java.io.Closeable

interface Editor: Closeable {

    fun onStateChanged(block: (State) -> Unit): Closeable

    fun setText(text: String)

    fun submit()

    interface State {
        val text: String
        val reset: Boolean
    }
}