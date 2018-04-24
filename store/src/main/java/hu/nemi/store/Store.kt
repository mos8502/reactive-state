package hu.nemi.store

typealias ActionCreator<StateType, ActionType> = (StateType) -> ActionType?

typealias AsyncActionCreator<StateType, ActionType> = (StateType, (ActionCreator<StateType, ActionType>) -> Unit) -> Unit

interface Dispatcher<in ActionType, out ReturnType> {
    fun dispatch(action: ActionType): ReturnType
}


interface Store<StateType, ActionType> : Dispatcher<ActionType, Unit>, Observable<StateType> {
    fun dispatch(actionCreator: ActionCreator<StateType, ActionType>)

    fun dispatch(asyncActionCreator: AsyncActionCreator<StateType, ActionType>)

    companion object {
        operator fun <StateType, ActionType> invoke(initialState: StateType,
                                                    reducer: (StateType, ActionType) -> StateType,
                                                    middlewares: Iterable<Middleware<StateType, ActionType>> = emptyList()): Store<StateType, ActionType> =
                StoreImpl(initialState = initialState, reducer = reducer, middlewares = middlewares)
    }
}

interface Middleware<StateType, ActionType> {
    fun dispatch(store: Dispatcher<ActionType, Unit>, state: StateType, action: ActionType, next: Dispatcher<ActionType, ActionType?>): ActionType?
}

fun <StateType, ActionType> Store(initialState: StateType,
                                  reducer: (StateType, ActionType) -> StateType,
                                  middlewares: Iterable<Middleware<StateType, ActionType>> = emptyList()): Store<StateType, ActionType> =
        StoreImpl(initialState, reducer, middlewares)

fun <StateType, ActionType> compose(vararg reducers: (StateType, ActionType) -> StateType): (StateType, ActionType) -> StateType {
    require(reducers.isNotEmpty()) { "no reducers passed" }
    return { state, action ->
        reducers.fold(state) { state, reducer ->
            reducer.invoke(state, action)
        }
    }
}

private class MiddlewareDispatcher<StateType, ActionType>(val state: StateType,
                                                          val store: Dispatcher<ActionType, Unit>,
                                                          middlewares: Iterable<Middleware<StateType, ActionType>>) : Dispatcher<ActionType, ActionType?> {
    private val middlewareIterator = middlewares.iterator()
    private val next = object : Dispatcher<ActionType, ActionType?> {
        override fun dispatch(action: ActionType): ActionType? {
            return if (middlewareIterator.hasNext()) middlewareIterator.next().dispatch(store = store, state = state, action = action, next = this)
            else action
        }

    }

    override fun dispatch(action: ActionType): ActionType? = next.dispatch(action)
}

private class StoreImpl<StateType, ActionType>(initialState: StateType,
                                               private val reducer: (StateType, ActionType) -> StateType,
                                               private val middlewares: Iterable<Middleware<StateType, ActionType>>) : Store<StateType, ActionType> {
    @Volatile
    private var state = initialState
    @Volatile
    private var subscribers = emptySet<(StateType) -> Unit>()
    @Volatile
    private var isDispatching = false
    private val lock = Lock()

    override fun dispatch(action: ActionType) = lock {
        require(!isDispatching) { "dispatch already in progress" }

        isDispatching = true
        val newState = try {
            val dispatcher = MiddlewareDispatcher(state = state, store = this, middlewares = middlewares)
            dispatcher.dispatch(action)?.let { dispatchedAction ->
                reducer.invoke(state, dispatchedAction)
            }
        } finally {
            isDispatching = false
        }

        if (newState != null) {
            state = newState
            subscribers.forEach { subscriber -> subscriber.invoke(state) }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<StateType, ActionType>) {
        actionCreator.invoke(state)?.let(::dispatch)
    }

    override fun dispatch(asyncActionCreator: AsyncActionCreator<StateType, ActionType>) = lock {
        asyncActionCreator(state) { actionCreator ->
            dispatch(actionCreator)
        }
    }

    override fun subscribe(block: (StateType) -> Unit): Subscription = lock {
        val subscribers = this.subscribers.toMutableSet()
        if (subscribers.add(block)) {
            this.subscribers = subscribers.toSet()
            block.invoke(state)
        }

        Subscription {
            subscribers -= block
        }
    }
}
