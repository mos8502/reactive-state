package hu.nemi.store

import java.util.concurrent.atomic.AtomicLong
import java.util.ConcurrentModificationException

typealias ActionCreator<StateType, ActionType> = (StateType) -> ActionType?

typealias AsyncActionCreator<StateType, ActionType> = (StateType, (ActionCreator<StateType, ActionType>) -> Unit) -> Unit

interface Dispatcher<in ActionType, out ReturnType> {
    fun dispatch(action: ActionType): ReturnType
}


interface Store<StateType, ActionType> : Dispatcher<ActionType, Unit>, Observable<StateType> {
    fun dispatch(actionCreator: ActionCreator<StateType, ActionType>)

    fun dispatch(asyncActionCreator: AsyncActionCreator<StateType, ActionType>)
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
    @Volatile private var state = initialState
    @Volatile private var subscribers = emptySet<(StateType) -> Unit>()
    @Volatile private var isDispatching = false
    private val lock = StoreLock()

    override fun dispatch(action: ActionType) = locked {
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

    override fun dispatch(asyncActionCreator: AsyncActionCreator<StateType, ActionType>) {
        locked {
            asyncActionCreator(state) { actionCreator ->
                dispatch(actionCreator)
            }
        }
    }

    override fun subscribe(block: (StateType) -> Unit): Subscription = locked {
        val subscribers = this.subscribers.toMutableSet()
        if (subscribers.add(block)) {
            this.subscribers = subscribers.toSet()
            block.invoke(state)
        }

        object : Subscription {
            override fun unsubscribe() {
                subscribers -= block
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