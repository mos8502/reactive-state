package hu.nemi.store.coroutines

import hu.nemi.store.Middleware
import hu.nemi.store.Store
import hu.nemi.store.StoreDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Get an instance of [StoreDispatcher] based on coroutines
 *
 * @param store the store to wrap in this dispatcher
 * @param messageContext context in which messages are dispatched
 * @param stateContext context in which state changes are dispatched
 */
fun <State, Message> coroutineDispatcher(store: Store<State, Message>,
                                         middlewareChain: Middleware.Chain<State, Message>,
                                         messageContext: CoroutineContext,
                                         stateContext: CoroutineContext): StoreDispatcher<State, Message> =
        CoroutineStoreDispatcherImpl(store, middlewareChain, messageContext, stateContext)

fun Job.toCloseable(): Closeable = CloseableJob(this)

private class CoroutineStoreDispatcherImpl<State, Message>(private val store: Store<State, Message>,
                                                           private val middlewareChain: Middleware.Chain<State, Message>,
                                                           private val messageDispatcher: CoroutineContext,
                                                           private val stateDispatcher: CoroutineContext) : StoreDispatcher<State, Message> {
    private val job = Job()
    private val _state = ConflatedBroadcastChannel<State>()
    private val actor = actor<Message>(context = messageDispatcher, parent = job) {
        store.onStateChanged { state -> _state.offer(state) }
        while (isActive) {
            select<Unit> {
                channel.onReceive { message ->
                    store.dispatch(message)
                    middlewareChain.dispatch(this@CoroutineStoreDispatcherImpl, _state.value, message)
                }
            }
        }
    }

    override fun dispatch(message: Message) {
        launch(context = messageDispatcher, parent = job) {
            actor.send(message)
        }
    }

    override fun subscribe(block: (State) -> Unit): Closeable {
        return launch(context = stateDispatcher, parent = job) {
            _state.consumeEach { state ->
                block(state)
            }
        }.toCloseable()
    }

    override fun close() {
        middlewareChain.close()
        job.cancel()
        _state.close()
    }
}

private class CloseableJob(private val job: Job) : Job by job, Closeable {
    override fun close() {
        job.cancel()
    }
}