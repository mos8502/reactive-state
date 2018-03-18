package hu.nemi.costate.notes.ui

interface BindableView<Model> {
    fun bind(model: Model)
}