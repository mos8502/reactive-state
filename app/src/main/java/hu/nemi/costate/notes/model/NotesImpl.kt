package hu.nemi.costate.notes.model

import hu.nemi.costate.arch.BlockViewModel
import hu.nemi.costate.notes.db.NoteEntity
import hu.nemi.costate.notes.usecases.*
import hu.nemi.store.Store
import hu.nemi.store.Subscription
import java.util.*
import javax.inject.Inject

class NotesViewModel @Inject constructor(private val notes: Notes) : BlockViewModel<ViewState, Notes>(notes), NotesApi by notes

class NotesImpl @Inject constructor(private val store: Store<@JvmSuppressWildcards State, @JvmSuppressWildcards Action>,
                                    private val loadNotes: LoadNotes,
                                    private val saveNote: SaveEdit,
                                    private val createNote: CreateNote,
                                    private val deleteNoteFactory: DeleteNote.Factory) : Notes {
    override fun loadNotes() = loadNotes.execute(store)

    override fun onActive() = loadNotes()

    override fun subscribe(block: (ViewState) -> Unit): Subscription =
            store.subscribe { block.invoke(it.toViewState()) }

    private fun State.toViewState(): ViewState {
        var items = entities.map { note ->
            val delete = ItemAction0(note) {
                deleteNoteFactory.create(it.id).execute(store)
            }
            val onClicked = ItemAction0(note) {
                editNote(it)
            }
            if (editor?.noteId == note.id) {
                ListItem.EditorItem(text = editor.text,
                        isSaving = editor.isSaving,
                        error = editor.error,
                        cancel = ItemAction0(editor) {
                            store.dispatch(Action.CancelEdit)
                        },
                        save = ItemAction0(editor) {
                            saveNote.execute(store)
                        },
                        setText = ItemAction1(editor) { _, text ->
                            store.dispatch(Action.UpdateEditorText(text))
                        },
                        id = note.id)
            } else {
                ListItem.NoteItem(id = note.id, text = note.text, delete = delete, onClicked = onClicked)
            }
        }

        if (editor == null) items += ListItem.AddItem {
            store.dispatch(Action.NewNote)
        }

        if (editor != null && editor.noteId == null) items += ListItem.EditorItem(text = editor.text,
                isSaving = editor.isSaving,
                error = editor.error,
                cancel = ItemAction0(editor) {
                    store.dispatch(Action.CancelEdit)
                },
                save = ItemAction0(editor) {
                    createNote.execute(store)
                },
                setText = ItemAction1(editor) { _, text ->
                    store.dispatch(Action.UpdateEditorText(text))
                },
                id = null)

        return ViewState(notes = items)
    }

    private fun editNote(it: NoteEntity) = store.dispatch(Action.EditNote(editorId = UUID.randomUUID().toString(), noteId = it.id))

    private class ItemAction0<E : Any>(private val entity: E, private val block: (E) -> Unit) : () -> Unit {
        override fun invoke() = block(entity)

        override fun hashCode(): Int = entity.hashCode()

        override fun equals(other: Any?): Boolean {
            return other is ItemAction0<*> && other.entity == this.entity
        }
    }

    private class ItemAction1<E : Any, A>(private val entity: E, private val block: (E, A) -> Unit) : (A) -> Unit {
        override fun invoke(arg: A) {
            block(entity, arg)
        }

        override fun hashCode(): Int = entity.hashCode()

        override fun equals(other: Any?): Boolean {
            return other is ItemAction1<*, *> && other.entity == this.entity
        }
    }
}