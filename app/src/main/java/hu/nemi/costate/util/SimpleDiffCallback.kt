package hu.nemi.costate.util

import android.support.v7.util.DiffUtil

fun <Model, Id> simpleDiffCallback(oldItems: List<Model>, newItems: List<Model>, id: Model.() -> Id): DiffUtil.Callback =
        SimpleDiffCallback(oldItems, newItems, id)

private class SimpleDiffCallback<Model, out Id>(val oldItem: List<Model>,
                                            val newItems: List<Model>,
                                            val id: Model.() -> Id) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItem[oldItemPosition].id() == newItems[newItemPosition].id()

    override fun getOldListSize() = oldItem.size

    override fun getNewListSize() = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItem[oldItemPosition] == newItems[newItemPosition]
}