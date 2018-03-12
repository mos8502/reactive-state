package hu.nemi.costate.model

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface Notes {
    val notes: ReceiveChannel<List<Note>>
    fun create(text: String): Job
    fun delete(note: Note): Job
    fun update(note: Note): Job
}
