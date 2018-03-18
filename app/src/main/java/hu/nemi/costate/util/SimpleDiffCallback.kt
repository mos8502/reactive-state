package hu.nemi.costate.util

import android.support.v7.util.DiffUtil

class SimpleDiffCallkback<Model, Id>(private val oldItems: List<Model>, private val newItems: List<Model>, val id: (Model) -> Id) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            id(oldItems[oldItemPosition]) == id(newItems[newItemPosition])

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItems[oldItemPosition] == newItems[newItemPosition]
}