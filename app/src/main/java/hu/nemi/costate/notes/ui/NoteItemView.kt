package hu.nemi.costate.notes.ui
import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import hu.nemi.costate.R
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.Notes.Item.Note

class NoteItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = android.R.attr.textViewStyle): AppCompatTextView(context, attrs, defStyle), BindableView<Notes.Item.Note> {
    private lateinit var model: Note
    init {
        val padding = resources.getDimensionPixelSize(R.dimen.key_line)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setPadding(padding, padding, padding, padding)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.line_item_text_size))
        gravity = Gravity.START or Gravity.CENTER_VERTICAL

        setOnClickListener { model.edit() }
    }

    override fun bind(model: Note) {
        this.model = model
        text = model.entity.text
    }
}