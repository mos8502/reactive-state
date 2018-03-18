package hu.nemi.costate.notes.ui

import android.content.Context
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import hu.nemi.costate.R
import hu.nemi.costate.notes.Editor
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.util.onEditorActionEvent
import hu.nemi.costate.util.onTextChangedEvent
import java.io.Closeable

class EditItemView(context: Context,
                   attrs: AttributeSet? = null,
                   defStyle: Int = R.attr.editTextStyle) : AppCompatEditText(context, attrs, defStyle), BindableView<Notes.Item.Edit> {
    private lateinit var model: Notes.Item.Edit
    private var stateSubscription: Closeable? = null
    private var textChangeSubscription: Closeable? = null
    private var editorActionSubscription: Closeable? = null

    init {
        val padding = resources.getDimensionPixelSize(R.dimen.key_line)
        setPadding(padding, padding, padding, padding)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setTextSize(COMPLEX_UNIT_PX, resources.getDimension(R.dimen.line_item_text_size))
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        imeOptions = IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        maxLines = Int.MAX_VALUE
        setSingleLine(true)
        setHorizontallyScrolling(false)
    }

    override fun bind(model: Notes.Item.Edit) {
        this.model = model
        requestFocus()
        showKeyboard()
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) model.editor.close()
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        stateSubscription = model.editor.onStateChanged(object: (Editor.State) -> Unit {
            private var isFirst = true
            override fun invoke(state: Editor.State) {
                if(isFirst || state.reset) setText(state.text)
                isFirst = false
            }
        })

        textChangeSubscription = onTextChangedEvent {
            model.editor.setText(it.toString())
        }

        editorActionSubscription = onEditorActionEvent { action, _ ->
            when (action) {
                IME_ACTION_DONE -> if (text.isEmpty()) {
                    model.editor.close()
                    false
                } else {
                    model.editor.submit()
                    true
                }
                else -> false
            }
        }
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if(event.keyCode == KeyEvent.KEYCODE_BACK) {
            hideKeyboard()
            model.editor.close()
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stateSubscription?.close()
        textChangeSubscription?.close()
        editorActionSubscription?.close()
        hideKeyboard()
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}