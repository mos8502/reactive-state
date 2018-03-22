package hu.nemi.costate.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import hu.nemi.costate.notes.NotesViewModel
import hu.nemi.costate.notes.impl.NotesViewModelImpl

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(type = NotesViewModel::class)
    abstract fun bindNotesViewModel(viewModel: NotesViewModelImpl): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}