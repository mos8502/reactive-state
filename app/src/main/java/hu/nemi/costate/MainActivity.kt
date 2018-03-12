package hu.nemi.costate

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import hu.nemi.costate.di.notesComponent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NotesViewModel
    private val adapter = NotesAdapter()
    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        private val rect = RectF()
        private val paint: Paint by lazy {
            Paint().apply {
                val ta = obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
                color = ta.getColor(0, -1)
                ta.recycle()
                style = Paint.Style.FILL
            }
        }
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val isDisplayItem = adapter.notes[viewHolder.adapterPosition] is NotesViewModel.LineItem.DisplayItem
            return ItemTouchHelper.Callback.makeMovementFlags(0, if(isDisplayItem) ItemTouchHelper.RIGHT else 0)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            viewModel.delete((adapter.notes[viewHolder.adapterPosition] as NotesViewModel.LineItem.DisplayItem).note)
        }

        override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0.0f) {
                rect.top = viewHolder.itemView.top.toFloat()
                rect.bottom = viewHolder.itemView.bottom.toFloat()
                rect.left = if (dX > 0) 0.0f else viewHolder.itemView.right - dX
                rect.right = if (dX > 0) dX else viewHolder.itemView.right.toFloat()
                canvas.drawRect(rect, paint)
            }

        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, notesComponent.viewModelFactory).get(NotesViewModel::class.java)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        notes.adapter = adapter
        notes.layoutManager = LinearLayoutManager(this)
        itemTouchHelper.attachToRecyclerView(notes)

        viewModel.state.observe(this, Observer<NotesViewModel.State> {
            adapter.notes = it?.notes ?: emptyList()
        })
    }
}
