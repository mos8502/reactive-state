package hu.nemi.costate.arch

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext

interface Store<State, Message>: Closeable {
    val state: SubscriptionReceiveChannel<State>
    fun dispatch(message: Message)
    fun dispatch(messages: ReceiveChannel<Message>)
}

interface Middleware<State, Message> {
    fun dispatch(store: Store<State, Message>, message: Message)
}

fun <State, Message> Store(initialState: State,
                           reduce: State.(Message) -> State,
                           middleware: Middleware<State, Message>? = null,
                           context: CoroutineContext = DefaultDispatcher): Store<State, Message> =
        DefaultStore(initialState, reduce, middleware, context)

private class DefaultStore<State, Message>(initialState: State,
                                   private val reduce: State.(Message) -> State,
                                   private val middleware: Middleware<State, Message>? = null,
                                   private val context: CoroutineContext = DefaultDispatcher): Store<State, Message> {
    private val job = Job()
    private val _state = ConflatedBroadcastChannel<State>()

    private val actor = actor<Message>(context = context, parent = job) {
        scan(initialState, reduce).distinct().consumeEach { _state.send(it) }

        withContext(NonCancellable) {
            _state.close(CancellationException())
        }
    }

    override val state: SubscriptionReceiveChannel<State>
        get() = _state.openSubscription()


    override fun dispatch(message: Message) {
        launch(context = context, parent = job) {
            actor.send(message)
        }
        middleware?.dispatch(this, message)

    }

    override fun dispatch(messages: ReceiveChannel<Message>) {
        launch(context = context, parent = job) {
            messages.consumeEach {
                actor.send(it)
                middleware?.dispatch(this@DefaultStore, it)
            }
        }
    }

    override fun close() {
        actor.close(CancellationException())
    }
}

private fun <S, E> ReceiveChannel<E>.scan(initialState: S, reduce: S.(E) -> S) : ReceiveChannel<S> {
    return produce {
        var state = initialState
        do {
            send(state)
            state = state.reduce(receive())
        } while (isActive)
    }
}