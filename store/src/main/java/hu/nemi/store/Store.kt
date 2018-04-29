package hu.nemi.store

import kotlin.properties.Delegates

interface ActionCreator<in S : Any, out A> {
    operator fun invoke(state: S): A?
}

interface AsyncActionCreator<in S : Any, out A : Any?> {
    operator fun invoke(state: S, dispatcher: (ActionCreator<S, A>) -> Unit)
}

interface Dispatcher<in S, out R> {
    fun dispatch(action: S): R
}

interface Store<S : Any, in A : Any> : Dispatcher<A, Unit>, Observable<S> {

    fun dispatch(actionCreator: ActionCreator<S, A>)

    fun dispatch(asyncActionCreator: AsyncActionCreator<S, A>)

    companion object {
        operator fun <S : Any, A : Any> invoke(initialState: S,
                                               reducer: (S, A) -> S,
                                               middleware: Iterable<Middleware<S, A>> = emptyList()): Store<S, A> =
                StateStore(initialState = initialState).withReducer(reducer, middleware)
    }
}

interface Middleware<in S, A> {
    fun dispatch(store: Dispatcher<A, Unit>, state: S, action: A, next: Dispatcher<A, A?>): A?
}

interface StateStore<S : Any> : Store<S, (S) -> S> {

    fun <R : Any> subStore(lens: Lens<S, R>): StateStore<R>

    fun <R : Any> subStore(key: Any, init: () -> R): StateStore<R>

    fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>> = emptyList()): Store<S, A>

    companion object {
        operator fun <S : Any> invoke(initialState: S): StateStore<S> =
                DefaultStateStore(rootStore = RootStore(initialState = initialState, lock = Lock()), parentNode = Node<S>(), parentLens = Lens())
    }
}

private class MiddlewareDispatcher<in S : Any, A>(private val store: Dispatcher<A, Unit>,
                                                  private val middleware: Iterable<Middleware<S, A>>) : Dispatcher<A, A?> {
    private lateinit var state: S

    fun onStateChanged(state: S) {
        this.state = state
    }

    override fun dispatch(action: A): A? = ActionDispatcher().dispatch(action)

    private inner class ActionDispatcher : Dispatcher<A, A?> {
        private val middlewareIterator = middleware.iterator()
        override fun dispatch(action: A): A? {
            return if (middlewareIterator.hasNext()) middlewareIterator.next().dispatch(store = store, state = state, action = action, next = this)
            else action
        }
    }
}

private class RootStore(initialState: Any, val lock: Lock) {
    private var state by Delegates.observable(StateNode(initialState)) { _, oldState, newState ->
        if (newState != oldState) subscriptions.keys.forEach { subscriber -> subscriber(newState) }
    }
    @Volatile private var subscriptions = emptyMap<(StateNode<Any>) -> Unit, Subscription>()
    @Volatile private var isDispatching = false

    fun dispatch(action: (StateNode<Any>) -> StateNode<Any>) = lock {
        if (isDispatching) throw IllegalStateException("an action is already being dispatched")

        isDispatching = true
        state = try {
            action(state)
        } finally {
            isDispatching = false
        }
    }

    fun dispatch(actionCreator: ActionCreator<StateNode<Any>, (StateNode<Any>) -> StateNode<Any>>) {
        lock {
            actionCreator(state)?.let(::dispatch)
        }
    }

    fun dispatch(asyncActionCreator: AsyncActionCreator<StateNode<Any>, (StateNode<Any>) -> StateNode<Any>>) = lock {
        asyncActionCreator(state) {
            dispatch(it)
        }
    }

    fun subscribe(block: (StateNode<Any>) -> Unit): Subscription = lock {
        var subscription = subscriptions[block]
        if (subscription == null) {
            subscription = Subscription {
                subscriptions -= block
            }
            subscriptions += block to subscription
            block(state)
        }
        subscription
    }
}

private class DefaultStateStore<S : Any, P : Any>(val rootStore: RootStore, val parentNode: Node<P>, val parentLens: Lens<P, S>) : StateStore<S> {
    override fun dispatch(action: (S) -> S) = rootStore.lock {
        rootStore.dispatch {
            parentNode.value.modify(it) {
                parentLens(it, action(parentLens(it)))
            }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<S, (S) -> S>) {
        rootStore.dispatch(object : ActionCreator<StateNode<Any>, (StateNode<Any>) -> StateNode<Any>> {
            override fun invoke(state: StateNode<Any>) = actionCreator(parentLens(parentNode.value(state)))?.let { action ->
                { state: StateNode<Any> ->
                    parentNode.value.modify(state) {
                        parentLens(it, action(parentLens(it)))
                    }
                }
            }
        })
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<S, (S) -> S>) = rootStore.lock {
        rootStore.dispatch(object : AsyncActionCreator<StateNode<Any>, (StateNode<Any>) -> StateNode<Any>> {
            override fun invoke(state: StateNode<Any>, dispatcher: (ActionCreator<StateNode<Any>, (StateNode<Any>) -> StateNode<Any>>) -> Unit) {
                asyncActionCreator(parentLens(parentNode.value(state))) {
                    dispatch(it)
                }
            }
        })
    }

    override fun subscribe(block: (S) -> Unit): Subscription = rootStore.lock {
        rootStore.subscribe(Subscriber(block, parentNode, parentLens))
    }

    override fun <R : Any> subStore(subLens: Lens<S, R>): StateStore<R> =
            DefaultStateStore(rootStore = rootStore, parentNode = Node(parentNode, parentLens), parentLens = subLens)

    override fun <R : Any> subStore(key: Any, init: () -> R): StateStore<R> =
            DefaultStateStore(rootStore = rootStore, parentNode = parentNode.withChild(key, init), parentLens = Lens())

    override fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>>): Store<S, A> = ReducerStore(this, reducer, middleware)

    private data class Subscriber<S : Any, P : Any>(val block: (S) -> Unit, val node: Node<P>, val lens: Lens<P, S>) : (StateNode<Any>) -> Unit {
        override fun invoke(state: StateNode<Any>) = block(lens(node.value(state)))
    }
}

private class ReducerStore<S : Any, in A : Any>(private val store: Store<S, (S) -> S>,
                                                private val reducer: (S, A) -> S,
                                                middleware: Iterable<Middleware<S, A>>) : Store<S, A> {
    private val middlewareDispatcher = MiddlewareDispatcher(this, middleware).apply {
        subscribe(::onStateChanged)
    }

    override fun subscribe(block: (S) -> Unit): Subscription = store.subscribe(block)

    override fun dispatch(action: A) {
        middlewareDispatcher.dispatch(action)?.let { dispatchedAction ->
            store.dispatch { reducer(it, dispatchedAction) }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<S, A>) {
        store.dispatch(object : ActionCreator<S, (S) -> S> {
            override fun invoke(state: S): ((S) -> S)? {
                return actionCreator(state)?.let { action ->
                    { reducer(state, action) }
                }
            }
        })
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<S, A>) {
        store.dispatch(object : AsyncActionCreator<S, (S) -> S> {
            override fun invoke(state: S, dispatcher: (ActionCreator<S, (S) -> S>) -> Unit) {
                asyncActionCreator(state) { actionCreator ->
                    dispatch(actionCreator)
                }
            }
        })
    }
}
