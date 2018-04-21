package hu.nemi.costate.di

import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NotesModule::class, ViewModelModule::class, CoroutinesModule::class, DatabaseModule::class])
interface NotesComponent {
    val viewModelFactory: ViewModelProvider.Factory

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun withContext(context: Context): Builder

        fun build(): NotesComponent
    }

    interface Holder {
        val component: NotesComponent
    }
}

val Context.notesComponent: NotesComponent
    get() = (applicationContext as NotesComponent.Holder).component