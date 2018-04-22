package hu.nemi.costate.notes.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.nemi.costate.R
import hu.nemi.costate.notes.model.ListItem

class AddView private constructor(override val view: View) : ListItemView {
    private var add: (() -> Unit)? = null

    init {
        view.setOnClickListener {
            add?.invoke()
        }
    }

    override fun bind(addItem: ListItem.AddItem) {
        add = addItem.add
    }

    companion object {
        operator fun invoke(parent: ViewGroup): AddView =
                AddView(LayoutInflater.from(parent.context).inflate(R.layout.view_add_item, parent, false))
    }
}