package hu.nemi.store

import com.nhaarman.mockito_kotlin.inOrder as mockitoInOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Test
import org.mockito.InOrder
import java.util.ConcurrentModificationException
import java.util.concurrent.CountDownLatch

class StoreTest {
    @Test
    fun `state emitted when subscribed`() {
        val listener = mockListener()
        val store = store(23, ::reducer)

        store.subscribe(listener)

        verify(listener).invoke(23)
    }

    @Test
    fun `state not emitted when not changed`() {
        val listener = mockListener()
        val store = store(23, ::reducer)

        store.subscribe(listener)
        store.dispatch(Opp.Set(23))

        verify(listener).invoke(23)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `state emitted when changed`() {
        val listener = mockListener()
        val store = store(23, ::reducer)

        store.subscribe(listener)
        store.dispatch(Opp.Set(13))

        inOrder(listener) {
            verify(listener).invoke(23)
            verify(listener).invoke(13)
        }
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `listener not invoked when unsubscribed`() {
        val listener = mockListener()
        val store = store(23, ::reducer)
        val subscription = store.subscribe(listener)

        store.dispatch(Opp.Inc)
        subscription.close()
        store.dispatch(Opp.Inc)

        inOrder(listener) {
            verify(listener).invoke(23)
            verify(listener).invoke(24)
        }
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `all subscribed listeners invoked on state change`() {
        val listener0 = mockListener()
        val listener1 = mockListener()
        val store = store(23, ::reducer)

        store.subscribe(listener0)
        store.subscribe(listener1)

        store.dispatch(Opp.Inc)

        inOrder(listener0) {
            verify(listener0).invoke(23)
            verify(listener0).invoke(24)
        }
        verifyNoMoreInteractions(listener0)

        inOrder(listener1) {
            verify(listener1).invoke(23)
            verify(listener1).invoke(24)
        }
        verifyNoMoreInteractions(listener1)
    }

    @Test
    fun `same listener is only registered once`() {
        val listener = mockListener()
        val store = store(23, ::reducer)
        val subscription0 = store.subscribe(listener)
        val subscription1 = store.subscribe(listener)

        subscription0.close()
        store.dispatch(Opp.Inc)
        subscription1.close()
        store.dispatch(Opp.Inc)

        verify(listener).invoke(23)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `store is reentrant`() {
        var counter = 5
        val store = store(23, ::reducer)

        class Listener : (Int) -> Unit {
            override fun invoke(state: Int) {
                if (--counter > 0) store.subscribe(Listener())
            }
        }
        store.subscribe(Listener())
        assert(counter == 0)
    }

    @Test(expected = ConcurrentModificationException::class)
    fun `error raised when trying to dispatch message while store is locked`() {
        val store = store(23, ::reducer)
        val lock = Object()
        val start = CountDownLatch(1)

        Thread {
            store.subscribe {
                start.countDown()
                synchronized(lock) {
                    lock.wait()
                }
            }
        }.start()

        start.await()
        store.dispatch(Opp.Inc)
    }

    @Test(expected = ConcurrentModificationException::class)
    fun `error raised when trying to register listener when store is locked`() {
        val store = store(23, ::reducer)
        val lock = Object()
        val start = CountDownLatch(1)

        Thread {
            store.subscribe {
                start.countDown()
                synchronized(lock) {
                    lock.wait()
                }
            }
        }.start()

        start.await()
        store.subscribe(mockListener())
    }

    @Test(expected = ConcurrentModificationException::class)
    fun `error raised when trying to unsubscribe listener when store is locked`() {
        val store = store(23, ::reducer)
        val lock = Object()
        val start = CountDownLatch(1)

        val subscription = store.subscribe(mockListener())
        Thread {
            store.subscribe {
                start.countDown()
                synchronized(lock) {
                    lock.wait()
                }
            }
        }.start()

        start.await()
        subscription.close()
    }

    private fun mockListener(): (Int) -> Unit = mock()

    private fun inOrder(vararg mocks: Any, block: (InOrder) -> Unit) = with(mockitoInOrder(*mocks), block)

    private sealed class Opp {
        object Inc : Opp()
        object Dec : Opp()
        data class Set(val value: Int) : Opp()
    }

    private fun reducer(state: Int, opp: Opp) = when (opp) {
        Opp.Inc -> state + 1
        Opp.Dec -> state - 1
        is Opp.Set -> opp.value
    }
}