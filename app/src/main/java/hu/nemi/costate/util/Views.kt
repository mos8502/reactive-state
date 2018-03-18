package hu.nemi.costate.util

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.TextView
import java.io.Closeable

fun TextView.onTextChangedEvent(block: (CharSequence) -> Unit): Closeable {
    val listener = TextChangeSubscription(this, block)
    addTextChangedListener(listener)
    return listener
}

fun TextView.onEditorActionEvent(block: (action: Int, keyEvent: KeyEvent?) -> Boolean) : Closeable {
    val listener = EditorActionSubscription(this, block)
    setOnEditorActionListener(listener)
    return listener
}

private class TextChangeSubscription(private val textView: TextView, private val block: (CharSequence) -> Unit): TextWatcher, Closeable {
    override fun afterTextChanged(text: Editable) {
        block(text)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

    override fun close() {
        textView.removeTextChangedListener(this)
    }
}

private class EditorActionSubscription(private val textView: TextView, private val block: (action: Int, keyEvent: KeyEvent?) -> Boolean): TextView.OnEditorActionListener, Closeable {
    override fun onEditorAction(text: TextView, action: Int, keyEvent: KeyEvent?): Boolean {
        return block(action, keyEvent)
    }

    override fun close() {
        textView.setOnEditorActionListener(null)
    }
}