package hu.nemi.store

import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicBoolean

interface MessageSink<Message> {
    fun dispatch(message: Message)
}

interface Store<State, Message> : MessageSink<Message> {
    /**
     * Set listener that will be invoked when the store state changes.
     */
    fun onStateChanged(block: (State) -> Unit)
}

interface Dispatcher<Message> : MessageSink<Message>, Closeable
/**
 * Contract for dispatchers that are responsible for dispatching messages
 * to a [Store] and allow subscription to store state changes
 */
interface StoreDispatcher<State, Message> : Dispatcher<Message> {
    /**
     * Subscribe to state changes
     *
     * @param block callback to bne invoked when state changes
     * @return subscription in the form  of a [Closeable]. Call [Closeable.close] to unsubscribe from state changes
     */
    fun subscribe(block: (State) -> Unit): Closeable
}

/**
 * Middleware contract to do side effecty things :)
 */
interface Middleware<State, Message> : Closeable {
    /**
     * Dispatch message to middleware
     *
     * @param dispatcher dispatcher to send messages to if needed
     * @param state the current state
     * @param message message being dispatched
     * @param next next item in dispatch chain
     */
    fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message, next: Chain.Next<State, Message>)

    /**
     * Contract for dispatch chain
     */
    interface Chain<State, Message> : Closeable {
        /**
         * Dispatch message to chain
         *
         * @param dispatcher dispatcher used to dispatch messages to the chain
         * @param state the current state
         * @param message message being dispatched
         */
        fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message)

        /**
         * Contract for items in the dispatch chain
         */
        interface Next<State, Message> {
            /**
             * Dispatch message to the next item
             *
             * @param dispatcher dispatcher used to dispatch messages
             * @param state the current state
             * @param message message being dispatched
             */
            fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message)
        }
    }
}

/**
 * Build an instance of [Middleware.Chain] from [Iterable<Middleware>]
 */
fun <State, Message> middlewareChain(middleware: Iterable<Middleware<State, Message>>): Middleware.Chain<State, Message> = MiddleWareChainImpl(middleware)

/**
 * Returns an implementation of [Store] that doesn't allow concurrent updates to a store.
 * In case of concurrent modifications call both to [Store.dispatch] and [Store.onStateChanged]
 * will throw [ConcurrentModificationException]
 *
 * @param initialState initial state of the store
 * @param reducer the reducer
 */
fun <State, Message> store(initialState: State,
                           reducer: (State, Message) -> State): Store<State, Message> = StoreImpl(initialState, reducer)

private class MiddleWareChainImpl<State, Message>(private val middleware: Iterable<Middleware<State, Message>>) : Middleware.Chain<State, Message> {

    override fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message) {
        MiddlewareChainNextImpl(middleware.iterator()).dispatch(DispatcherRef(dispatcher), state, message)
    }

    override fun close() = middleware.forEach { it.close() }
}

private class DispatcherRef<Message>(target: Dispatcher<Message>) : WeakReference<Dispatcher<Message>>(target), Dispatcher<Message> {
    override fun dispatch(message: Message) {
        get()?.dispatch(message)
    }

    override fun close() {
        get()?.close()
    }
}

private class MiddlewareChainNextImpl<State, Message>(private val iterator: Iterator<Middleware<State, Message>>) : Middleware.Chain.Next<State, Message> {
    override fun dispatch(dispatcher: Dispatcher<Message>, state: State, message: Message) {
        if (iterator.hasNext()) {
            iterator.next().dispatch(dispatcher, state, message, this)
        }
    }
}

private class StoreImpl<State, Message>(initialState: State,
                                        val reduce: (State, Message) -> State) : Store<State, Message> {
    private val inTransaction = AtomicBoolean(false)
    @Volatile
    private var state: State = initialState
    @Volatile
    private var listener: (State) -> Unit = {}

    override fun onStateChanged(block: (State) -> Unit) = inTransaction {
        if (listener != block) {
            listener = block
            block(state)
        }
    }

    override fun dispatch(message: Message) = inTransaction {
        val newState = reduce(state, message)
        if (newState != state) {
            state = newState
            listener(newState)
        }
    }

    private fun <R> inTransaction(block: () -> R): R {
        beginTransaction()
        try {
            return block()
        } finally {
            endTransaction()
        }
    }

    private fun beginTransaction() {
        if (!inTransaction.compareAndSet(false, true)) {
            throw ConcurrentModificationException()
        }
    }

    private fun endTransaction() {
        if (!inTransaction.compareAndSet(true, false)) {
            throw ConcurrentModificationException()
        }
    }
}
