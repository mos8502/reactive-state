package hu.nemi.costate

import android.app.Application
import hu.nemi.costate.di.DaggerNotesComponent
import hu.nemi.costate.di.NotesComponent

class NotesApplication: Application(), NotesComponent.Holder {
    override val component: NotesComponent by lazy {
        DaggerNotesComponent.builder().context(this).build()
    }
}