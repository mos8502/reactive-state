package hu.nemi.costate

interface LineItemView<in T: NotesViewModel.LineItem> {
    fun bind(lineItem: T)
    fun unbind() {}
}