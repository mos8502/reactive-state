package hu.nemi.costate

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import hu.nemi.costate.NotesViewModel.LineItem.DisplayItem

class NoteDisplayItemView(context: Context,
                          attrs: AttributeSet? = null,
                          defStyle: Int = android.R.attr.textViewStyle) : AppCompatTextView(context, attrs, defStyle), LineItemView<DisplayItem> {
    private lateinit var lineItem: DisplayItem
    init {
        val padding = resources.getDimensionPixelSize(R.dimen.key_line)
        setPadding(padding, padding, padding, padding)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        setTextSize(COMPLEX_UNIT_PX, resources.getDimension(R.dimen.line_item_text_size))
        gravity = Gravity.START or Gravity.CENTER_VERTICAL

        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        background = ta.getDrawable(0)
        ta.recycle()

        setOnClickListener {
            lineItem.edit()
        }
    }

    override fun bind(lineItem: DisplayItem) {
        this.lineItem = lineItem
        text = lineItem.note.text
    }
}