package hu.nemi.store

import java.util.concurrent.atomic.AtomicLong
import java.util.ConcurrentModificationException

typealias ActionCreator<StateType, ActionType> = (StateType) -> Optional<ActionType>

interface AsyncActionCreator<StateType, ActionType, ReturnType> : ActionCreator<StateType, ActionType> {
    fun onCreated(state: StateType): ReturnType
}

interface Dispatcher<in ActionType, out ReturnType> {
    fun dispatch(action: ActionType): ReturnType
}


interface Store<StateType, ActionType> : Dispatcher<ActionType, Unit>, Observable<StateType> {
    fun dispatch(actionCreator: ActionCreator<StateType, ActionType>)

    fun <ReturnType> dispatch(actionCreator: AsyncActionCreator<StateType, ActionType, ReturnType>): Optional<ReturnType>
}

interface Middleware<StateType, ActionType> {
    fun dispatch(store: Dispatcher<ActionType, Unit>, state: StateType, action: ActionType, next: Dispatcher<ActionType, Optional<ActionType>>): Optional<ActionType>
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
                                                          middlewares: Iterable<Middleware<StateType, ActionType>>) : Dispatcher<ActionType, Optional<ActionType>> {
    private val middlewareIterator = middlewares.iterator()
    private val next = object : Dispatcher<ActionType, Optional<ActionType>> {
        override fun dispatch(action: ActionType): Optional<ActionType> {
            return if (middlewareIterator.hasNext()) middlewareIterator.next().dispatch(store = store, state = state, action = action, next = this)
            else action.asOptional()
        }

    }

    override fun dispatch(action: ActionType): Optional<ActionType> = next.dispatch(action)
}

private class StoreImpl<StateType, ActionType>(initialState: StateType,
                                               private val reducer: (StateType, ActionType) -> StateType,
                                               private val middlewares: Iterable<Middleware<StateType, ActionType>>) : Store<StateType, ActionType> {
    @Volatile private var state = initialState
    @Volatile private var subscribers = emptySet<(StateType) -> Unit>()
    @Volatile private var isDispatching = false
    private val lock = StoreLock()

    override fun dispatch(action: ActionType) = locked {
        require(!isDispatching) { "dispatch already in progress" }

        isDispatching = true
        val newState = try {
            val dispatcher = MiddlewareDispatcher(state = state, store = this, middlewares = middlewares)
            dispatcher.dispatch(action).map { dispatchedAction ->
                reducer.invoke(state, dispatchedAction)
            }
        } finally {
            isDispatching = false
        }

        if (newState != Optional.Empty) {
            state = newState.getOrThrow()
            subscribers.forEach { subscriber -> subscriber.invoke(state) }
        }
    }

    override fun dispatch(actionCreator: ActionCreator<StateType, ActionType>) {
        actionCreator.invoke(state).map(::dispatch)
    }

    override fun <ReturnType> dispatch(actionCreator: AsyncActionCreator<StateType, ActionType, ReturnType>) = locked {
        actionCreator.invoke(state)
                .map { action ->
                    dispatch(action)
                    state
                }
                .map { state ->
                    actionCreator.onCreated(state)
                }
    }

    override fun subscribe(subscriber: (StateType) -> Unit): Subscription = locked {
        val subscribers = this.subscribers.toMutableSet()
        if (subscribers.add(subscriber)) {
            this.subscribers = subscribers.toSet()
            subscriber.invoke(state)
        }

        object : Subscription {
            override fun unsubscribe() {
                subscribers -= subscriber
            }
        }
    }

    private fun <R> locked(block: () -> R): R {
        val lock = this.lock.acquire()
        return try {
            block()
        } finally {
            lock.release()
        }
    }
}

private class StoreLock {
    private val accessingThread = AtomicLong(-1L)
    private val lock = object : Lock {
        override fun release() {
            if (accessingThread.get() != Thread.currentThread().id) throw ConcurrentModificationException()
            if (--accessCount == 0) accessingThread.set(-1L)
        }
    }

    @Volatile
    private var accessCount = 0

    fun acquire(): Lock {
        val threadId = Thread.currentThread().id
        if (accessingThread.get() == threadId || accessingThread.compareAndSet(-1L, threadId)) {
            ++accessCount
            return lock
        } else {
            throw ConcurrentModificationException()
        }
    }

    interface Lock {
        fun release()
    }
}