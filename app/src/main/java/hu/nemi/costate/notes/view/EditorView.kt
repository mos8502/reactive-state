package hu.nemi.costate.notes.view

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.nemi.costate.R
import hu.nemi.costate.notes.model.ListItem
import hu.nemi.costate.util.hideSoftKeyboard
import hu.nemi.costate.util.toggleSoftKeyboard
import kotlinx.android.synthetic.main.view_note_editor.view.*

class EditorView private constructor(override val view: View) : ListItemView, View.OnAttachStateChangeListener {
    private val editText = view.editText
    private val actionSave = view.actionSave
    private val actionCancel = view.actionCancel
    private var editor: ListItem.EditorItem? = null
    private val onParentScrollStateListener = object: RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            if(editText.text.isEmpty() && newState == RecyclerView.SCROLL_STATE_DRAGGING) cancel()
        }
    }
    private val parent: RecyclerView?
        get() = view.parent as? RecyclerView

    init {
        actionSave.setOnClickListener {
            editText.hideSoftKeyboard()
            editor?.save?.invoke()
        }
        actionCancel.setOnClickListener {
            cancel()
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable) {
                editor?.setText?.invoke(text.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
        })

        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        parent?.removeOnScrollListener(onParentScrollStateListener)
    }

    override fun onViewAttachedToWindow(view: View) {
        parent?.addOnScrollListener(onParentScrollStateListener)
    }

    override fun bind(editorItem: ListItem.EditorItem) {
        editText.isEnabled = !editorItem.isSaving
        actionSave.visibility = if (editorItem.isSaving) View.GONE else View.VISIBLE
        actionCancel.visibility = if (editorItem.isSaving) View.GONE else View.VISIBLE
        editText.error = editorItem.error?.message
        if (editor == null) {
            editText.setText(editorItem.text)
            editText.post {
                editText.requestFocus()
                editText.toggleSoftKeyboard()
            }
        }
        editor = editorItem
    }

    override fun unbind() {
        editor = null
    }

    private fun cancel() {
        editText.hideSoftKeyboard()
        editor?.cancel?.invoke()
    }

    companion object {
        operator fun invoke(parent: ViewGroup): EditorView =
                EditorView(LayoutInflater.from(parent.context).inflate(R.layout.view_note_editor, parent, false))
    }

}