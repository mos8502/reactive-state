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
        val childStore = store.subState("string") { "Hello, World!" }
                .map(Lens(
                        get = { it.state },
                        set = { value: String -> { state: State<Int, String> -> state.copy(state = value) } }
                ))
        val grandchildSubscriber: (Long) -> Unit = mock()
        val grandChildStore = childStore.subState("long") { -1L }
                .map(Lens(
                        get = { it.state },
                        set = { value: Long -> { state: State<String, Long> -> state.copy(state = value) } }
                ))

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

    @Test
    fun `deep states`() {
        data class First(val root: State<State<State<State<Int, String>, Int>, Long>, Unit>, val fifth: Pair<Int, Int>)
        data class Second(val root: State<State<State<Int, String>, Int>, Long>, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class Third(val root: State<State<Int, String>, Int>, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class Fourth(val root: State<Int, String>, val second: Int, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)
        data class FlattenedState(val root: Int, val first: String, val second: Int, val third: Long, val fourth: Unit, val fifth: Pair<Int, Int>)

        val flatten =
                Lens<State<State<State<State<State<Int, String>, Int>, Long>, Unit>, Pair<Int, Int>>, First>(
                        get = { First(root = it.parentState, fifth = it.state) },
                        set = { first -> { state -> state.copy(parentState = first.root) } }
                ) +
                        Lens<First, Second>(
                                get = { Second(root = it.root.parentState, fourth = it.root.state, fifth = it.fifth) },
                                set = { second -> { first -> first.copy(root = State(second.root, second.fourth), fifth = second.fifth) } }
                        ) +
                        Lens<Second, Third>(
                                get = { Third(root = it.root.parentState, third = it.root.state, fourth = it.fourth, fifth = it.fifth) },
                                set = { third -> { second -> second.copy(root = State(third.root, third.third), fourth = third.fourth, fifth = third.fifth) } }
                        ) +
                        Lens<Third, Fourth>(
                                get = { Fourth(root = it.root.parentState, second = it.root.state, third = it.third, fourth = it.fourth, fifth = it.fifth) },
                                set = { fourth -> { third -> third.copy(root = State(fourth.root, fourth.second), third = fourth.third, fourth = fourth.fourth, fifth = fourth.fifth) } }
                        ) +
                        Lens<Fourth, FlattenedState>(
                                get = { FlattenedState(root = it.root.parentState, first = it.root.state, second = it.second, third = it.third, fourth = it.fourth, fifth = it.fifth) },
                                set = { state -> { fourth -> fourth.copy(root = State(state.root, state.first), second = state.second, third = state.third, fourth = state.fourth, fifth = state.fifth) } }
                        )

        val store = StateStore(23)
                .subState("first") { "1" }
                .subState("second") { 2 }
                .subState("third") { 3L }
                .subState(4) { Unit }
                .subState(5L) { Pair(1, 2) }
        val subscriber: (State<State<State<State<State<Int, String>, Int>, Long>, Unit>, Pair<Int, Int>>) -> Unit = mock()
        val expected = State(State(State(State(State(23, "1"), 2), 3L), Unit), 1 to 2)

        val mappedStore = store.map(flatten)
        val mappedSubscriber: (FlattenedState) -> Unit = mock()

        store.subscribe(subscriber)
        mappedStore.subscribe(mappedSubscriber)
        verify(subscriber)(expected)
        verify(mappedSubscriber)(flatten(expected))

    }
}