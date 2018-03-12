package hu.nemi.costate
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class AddItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0): ConstraintLayout(context, attrs, defStyle), LineItemView<NotesViewModel.LineItem.AddItem> {
    private lateinit var lineItem: NotesViewModel.LineItem.AddItem

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.view_add_item, this, true)

        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        background = ta.getDrawable(0)
        ta.recycle()

        setOnClickListener {
            lineItem.onCreate()
        }
    }

    override fun bind(lineItem: NotesViewModel.LineItem.AddItem) {
        this.lineItem = lineItem
    }
}