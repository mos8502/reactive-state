package hu.nemi.store

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

class StateStoreTest {

    @Test
    fun `state immediately emitted when subscribed`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)

        verify(subscriber).invoke(23)
        verifyNoMoreInteractions(subscriber)
    }

    @Test
    fun `subscriber not notified after unsubscribing`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subscription = store.subscribe(subscriber)

        subscription.unsubscribe()
        store.dispatch { it * 2 }

        verify(subscriber).invoke(23)
        verifyNoMoreInteractions(subscriber)
    }

    @Test
    fun `subscribers gets notified if state changes`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch { it * 2 }

        inOrder(subscriber) {
            verify(subscriber).invoke(23)
            verify(subscriber).invoke(46)
            verifyNoMoreInteractions(subscriber)
        }
    }

    @Test
    fun `state not emitted when not changed`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch { 23 }

        inOrder(subscriber) {
            verify(subscriber).invoke(23)
            verifyNoMoreInteractions(subscriber)
        }
    }

    @Test
    fun `the same subscription is returned for the same subscriber`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subscription0 = store.subscribe(subscriber)
        val subscription1 = store.subscribe(subscriber)

        assertThat(subscription0).isEqualTo(subscription1)
    }

    @Test
    fun `store dispatches action returned by action creator`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch(object : ActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int): ((Int) -> Int)? = { it * 2 }
        })

        inOrder(subscriber) {
            verify(subscriber).invoke(23)
            verify(subscriber).invoke(46)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `state not changed if action creator returns null`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch(object : ActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int): ((Int) -> Int)? = null
        })

        inOrder(subscriber) {
            verify(subscriber).invoke(23)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `all actions dispatched from async action creator`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)

        store.subscribe(subscriber)
        store.dispatch(object : AsyncActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int, dispatcher: (ActionCreator<Int, (Int) -> Int>) -> Unit) {
                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { it * 2 }
                })

                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { it - 13 }
                })
            }
        })

        inOrder(subscriber) {
            verify(subscriber).invoke(23)
            verify(subscriber).invoke(46)
            verify(subscriber).invoke(33)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `can have child state`() {
        val subscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val childSubscriber: (String) -> Unit = mock()
        val childStore = store.subStore("string") { "Hello, World!" }
        val grandchildSubscriber: (Long) -> Unit = mock()
        val grandChildStore = childStore.subStore("long") { -1L }

        store.subscribe(subscriber)
        childStore.subscribe(childSubscriber)
        grandChildStore.subscribe(grandchildSubscriber)

        store.dispatch { it * 2 }
        childStore.dispatch { "Goodbye, World!" }
        grandChildStore.dispatch { it + 23L }

        inOrder(subscriber, childSubscriber, grandchildSubscriber) {
            verify(subscriber).invoke(23)
            verify(childSubscriber).invoke("Hello, World!")
            verify(grandchildSubscriber).invoke(-1L)
            verify(subscriber).invoke(46)
            verify(childSubscriber).invoke("Goodbye, World!")
            verify(grandchildSubscriber).invoke(22L)
        }
    }
}