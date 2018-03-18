package hu.nemi.costate.notes.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import hu.nemi.costate.R
import hu.nemi.costate.notes.Notes

class CreateItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ConstraintLayout(context, attrs, defStyle), BindableView<Notes.Item.Create> {
    private lateinit var model: Notes.Item.Create

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.view_add_item, this, true)

        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        background = ta.getDrawable(0)
        ta.recycle()

        setOnClickListener {
            model.create()
        }
    }

    override fun bind(model: Notes.Item.Create) {
        this.model = model
    }
}