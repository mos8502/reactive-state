package hu.nemi.costate

import android.content.Context
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
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
import hu.nemi.costate.NotesViewModel.LineItem.EditItem
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.filter
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch

class NoteEditItemView(context: Context,
                       attrs: AttributeSet? = null,
                       defStyle: Int = R.attr.editTextStyle) : AppCompatEditText(context, attrs, defStyle), LineItemView<EditItem> {
    private lateinit var editor: Editor
    private var job: Job? = null
    private val onTextChanged = object: TextWatcher {
        override fun afterTextChanged(text: Editable) {
            editor.setText(text.toString())
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
    }
    private val onEditorAction = object : OnEditorActionListener {
        override fun onEditorAction(view: TextView, action: Int, keyEvent: KeyEvent?) = when {
            action == IME_ACTION_DONE -> if (text.isEmpty()) {
                editor.finish()
                false
            } else {
                editor.submit()
                true
            }
            keyEvent?.keyCode == KEYCODE_BACK -> {
                editor.finish()
                false
            }
            else -> false
        }
    }

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

    override fun bind(lineItem: EditItem) {
        text = null
        this.editor = lineItem.editor
        requestFocus()
        showKeyboard()
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) editor.finish()
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = launch(UI) {
            val state = editor.state
            setTextKeepState(state.receive().text)
            for(text in state.filter { it.reset }.map { it.text }) {
                setTextKeepState(text)
            }
        }

        addTextChangedListener(onTextChanged)
        setOnEditorActionListener(onEditorAction)
        onDrag {
            if(text.isEmpty()) editor.finish()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeTextChangedListener(onTextChanged)
        setOnEditorActionListener(null)
        hideKeyboard()
        job?.cancel()
    }

    private fun onDrag(callback: () -> Unit) {
        (parent as? RecyclerView)?.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if(newState == RecyclerView.SCROLL_STATE_DRAGGING) callback()
            }
        })
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