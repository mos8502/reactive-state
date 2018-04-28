package hu.nemi.store

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
                DefaultStateStore(initialState = initialState, lock = Lock()).withReducer(reducer, middleware)
    }
}

interface Middleware<in S, A> {
    fun dispatch(store: Dispatcher<A, Unit>, state: S, action: A, next: Dispatcher<A, A?>): A?
}

interface StateStore<S : Any> : Store<S, (S) -> S> {

    fun <R : Any> subStore(lens: Lens<S, R>): StateStore<R>

    fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>> = emptyList()): Store<S, A>

    companion object {
        operator fun <S : Any> invoke(initialState: S): StateStore<S> =
                DefaultStateStore(initialState = initialState, lock = Lock())
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

private class DefaultStateStore<S : Any>(initialState: S, private val lock: Lock) : StateStore<S> {
    @Volatile private var state = initialState
    @Volatile var subscriptions = emptyMap<(S) -> Unit, Subscription>()
    @Volatile var isDispatching = false

    override fun dispatch(action: (S) -> S) = lock {
        if (isDispatching) throw IllegalStateException("an action is already being dispatched")

        isDispatching = true

        val changedState = try {
            val newState = action(state)

            if (newState != state) {
                state = newState
                newState
            } else {
                null
            }
        } finally {
            isDispatching = false
        }

        if (changedState != null) {
            subscriptions.keys.forEach { subscriber -> subscriber(changedState) }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<S, (S) -> S>) {
        lock {
            actionCreator(state)?.let { dispatch(it) }
        }
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<S, (S) -> S>) = lock {
        asyncActionCreator(state) { actionCreator ->
            dispatch(actionCreator)
        }
    }

    override fun subscribe(block: (S) -> Unit): Subscription = lock {
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

    override fun <R : Any> subStore(lens: Lens<S, R>): StateStore<R> = SubStore(this, lens)

    override fun <A : Any> withReducer(reducer: (S, A) -> S, middleware: Iterable<Middleware<S, A>>): Store<S, A> = ReducerStore(this, reducer, middleware)
}

private class SubStore<R : Any, S : Any>(private val parent: StateStore<S>,
                                         private val lens: Lens<S, R>) : StateStore<R> {
    override fun dispatch(action: (R) -> R) = parent.dispatch { lens.set(it, action(lens.get(it))) }

    override fun dispatch(actionCreator: ActionCreator<R, (R) -> R>) {
        parent.dispatch {
            var subState = lens.get(it)
            subState = actionCreator(subState)?.invoke(subState) ?: subState
            lens.set(it, subState)
        }
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<R, (R) -> R>) {
        parent.dispatch(object : AsyncActionCreator<S, (S) -> S> {
            override fun invoke(state: S, dispatcher: (ActionCreator<S, (S) -> S>) -> Unit) {
                asyncActionCreator(lens.get(state)) { actionCreator ->
                    dispatch(actionCreator)
                }
            }
        })
    }

    override fun <P : Any> subStore(lens: Lens<R, P>): StateStore<P> = SubStore(this, lens)

    override fun subscribe(block: (R) -> Unit): Subscription = parent.subscribe { block(lens.get(it)) }

    override fun <A : Any> withReducer(reducer: (R, A) -> R, middleware: Iterable<Middleware<R, A>>): Store<R, A> = ReducerStore(this, reducer, middleware)
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