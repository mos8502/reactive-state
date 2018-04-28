package hu.nemi.store

import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test

class SubStoreTest {
    private val lens = object : Lens<Int, String> {
        override fun get(t: Int): String = t.toString()

        override fun set(t: Int, v: String): Int = v.toInt()
    }

    @Test
    fun `sub store emits state when changed`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch { it * 2 }
        subStore.dispatch { "13" }

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(46)
            verify(subStoreSubscriber).invoke("46")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
        }
    }

    @Test
    fun `can unsubscribe from sub store`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        val subStoreSubscription = subStore.subscribe(subStoreSubscriber)
        subStoreSubscription.unsubscribe()
        subStore.dispatch { "13" }

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to sub store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        subStore.dispatch(object : ActionCreator<String, (String) -> String> {
            override fun invoke(state: String): ((String) -> String)? = { "13" }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch(object : ActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int): ((Int) -> Int)? = { 13 }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `actions dispatched by async action creator to sub store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        subStore.dispatch(object : AsyncActionCreator<(String), (String) -> String> {
            override fun invoke(state: String, dispatcher: (ActionCreator<String, (String) -> String>) -> Unit) {
                dispatcher(object : ActionCreator<String, (String) -> String> {
                    override fun invoke(state: String): ((String) -> String)? = { "13" }
                })

                dispatcher(object : ActionCreator<String, (String) -> String> {
                    override fun invoke(state: String): ((String) -> String)? = { "2" }
                })
            }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verify(storeSubscriber).invoke(2)
            verify(subStoreSubscriber).invoke("2")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `actions dispatched by async action creator to store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.subStore(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch(object : AsyncActionCreator<(Int), (Int) -> Int> {
            override fun invoke(state: Int, dispatcher: (ActionCreator<Int, (Int) -> Int>) -> Unit) {
                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { 13 }
                })

                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { 2 }
                })
            }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verify(storeSubscriber).invoke(2)
            verify(subStoreSubscriber).invoke("2")
            verifyNoMoreInteractions()
        }
    }
}