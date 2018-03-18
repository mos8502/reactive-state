package hu.nemi.costate.notes.ui

import android.arch.lifecycle.ViewModelProviders
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import hu.nemi.costate.R
import hu.nemi.costate.di.notesComponent
import hu.nemi.costate.notes.Notes
import hu.nemi.costate.notes.NotesViewModel
import kotlinx.android.synthetic.main.content_main.*
import java.io.Closeable

class NotesActivity : AppCompatActivity() {
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
            val item = adapter.notes[viewHolder.adapterPosition]

            return ItemTouchHelper.Callback.makeMovementFlags(0, if(item is Notes.Item.Note) ItemTouchHelper.RIGHT else 0)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            (adapter.notes[viewHolder.adapterPosition] as Notes.Item.Note).delete()
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
    private lateinit var subscription: Closeable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notes.adapter = adapter
        notes.layoutManager = LinearLayoutManager(this)
        itemTouchHelper.attachToRecyclerView(notes)

        subscription = ViewModelProviders.of(this, notesComponent.viewModelFactory)
                .get(NotesViewModel::class.java)
                .onNotesChanged {
                    adapter.notes = it.notes
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription.close()
    }
}
