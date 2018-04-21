package hu.nemi.costate.notes.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import hu.nemi.costate.R
import hu.nemi.costate.di.notesComponent
import hu.nemi.costate.notes.model.NotesViewModel
import hu.nemi.costate.notes.model.ViewState
import kotlinx.android.synthetic.main.content_main.*

class NotesActivity : AppCompatActivity() {
    private lateinit var viewModel: NotesViewModel
    private val adapter = NotesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        notes.layoutManager = LinearLayoutManager(this)
        notes.adapter = adapter

        viewModel = ViewModelProviders.of(this, notesComponent.viewModelFactory)
                .get(NotesViewModel::class.java)

        viewModel.state.observe(this, Observer<ViewState> {
            render(it!!)
        })
    }

    private fun render(viewState: ViewState) {
        adapter.submitList(viewState.notes)
    }
}
