package hu.nemi.store

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test

class ReducerStoreTest {

    @Test
    fun `action dispatched to reducer store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        reducerStore.dispatch(Op.Inc)

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verify(reducerStoreSubscriber).invoke(24)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action dispatched to state store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        store.dispatch { it * 2 }

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(46)
            verify(reducerStoreSubscriber).invoke(46)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to reducer store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        reducerStore.dispatch(object : ActionCreator<Int, Op> {
            override fun invoke(state: Int): Op? = Op.Inc
        })

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verify(reducerStoreSubscriber).invoke(24)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to state store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        store.dispatch(object : ActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int): ((Int) -> Int)? = { it * 2 }
        })

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(46)
            verify(reducerStoreSubscriber).invoke(46)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creators dispatched by async action creator to reducer store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        reducerStore.dispatch(object : AsyncActionCreator<Int, Op> {
            override fun invoke(state: Int, dispatcher: (ActionCreator<Int, Op>) -> Unit) {
                dispatcher(object : ActionCreator<Int, Op> {
                    override fun invoke(state: Int): Op? = Op.Inc
                })

                dispatcher(object : ActionCreator<Int, Op> {
                    override fun invoke(state: Int): Op? = Op.Inc
                })

                dispatcher(object : ActionCreator<Int, Op> {
                    override fun invoke(state: Int): Op? = Op.Set(-1)
                })
            }
        })

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verify(reducerStoreSubscriber).invoke(24)
            verify(storeSubscriber).invoke(25)
            verify(reducerStoreSubscriber).invoke(25)
            verify(storeSubscriber).invoke(-1)
            verify(reducerStoreSubscriber).invoke(-1)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creators dispatched by async action creator to state store updates state`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber)
        store.dispatch(object : AsyncActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int, dispatcher: (ActionCreator<Int, (Int) -> Int>) -> Unit) {
                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { it + 1 }
                })

                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { it + 1 }
                })

                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { -1 }
                })
            }
        })

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verify(reducerStoreSubscriber).invoke(24)
            verify(storeSubscriber).invoke(25)
            verify(reducerStoreSubscriber).invoke(25)
            verify(storeSubscriber).invoke(-1)
            verify(reducerStoreSubscriber).invoke(-1)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `state changes not dispatched to reducer store after unsubscribed`() {
        val store = StateStore(23)
        val reducerStore = store.withReducer(reducer = reducer)
        val storeSubscriber: (Int) -> Unit = mock()
        val reducerStoreSubscriber: (Int) -> Unit = mock()

        store.subscribe(storeSubscriber)
        reducerStore.subscribe(reducerStoreSubscriber).unsubscribe()
        reducerStore.dispatch(Op.Inc)

        inOrder(storeSubscriber, reducerStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(reducerStoreSubscriber).invoke(23)
            verify(storeSubscriber).invoke(24)
            verifyNoMoreInteractions()
        }
    }
}

private val reducer: (Int, Op) -> Int = { state, action ->
    when (action) {
        Op.Inc -> state + 1
        Op.Dec -> state - 1
        is Op.Set -> action.value
    }
}

private sealed class Op {
    object Inc : Op()
    object Dec : Op()
    data class Set(val value: Int) : Op()
}