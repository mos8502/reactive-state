package hu.nemi.store.rxjava2

import hu.nemi.store.Middleware
import hu.nemi.store.Store
import hu.nemi.store.StoreDispatcher
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.io.Closeable

/**
 * Get an instance of [StoreDispatcher] based on rxjava
 *
 * @param store the store to wrap in this dispatcher
 * @param messageDispatcher [Scheduler] on which messages are dispatched
 * @param stateContext [Scheduler] on which state changes are delivered
 */
fun <State, Message> rxDispatcher(store: Store<State, Message>,
                                  middlewareChain: Middleware.Chain<State, Message>,
                                  messageDispatcher: Scheduler,
                                  stateDispatcher: Scheduler): StoreDispatcher<State, Message> =
        RxStoreDispatcher(store, middlewareChain, messageDispatcher, stateDispatcher)

private class RxStoreDispatcher<State, Message>(val store: Store<State, Message>,
                                                middlewareChain: Middleware.Chain<State, Message>,
                                                messageDispatcher: Scheduler,
                                                val stateDispatcher: Scheduler) : StoreDispatcher<State, Message> {
    private val messages = PublishSubject.create<Message>().toSerialized()
    private val state = BehaviorSubject.create<State>()
    private val disposable: Disposable

    init {
        store.subscribe(state::onNext)
        disposable = messages.withLatestFrom(state, BiFunction<Message, State, Pair<Message, State>> { message, state -> Pair(message, state) })
                .observeOn(messageDispatcher)
                .subscribe {
                    store.dispatch(it.first)
                    middlewareChain.dispatch(this, it.second, it.first)
                }
    }

    override fun dispatch(message: Message) {
        messages.onNext(message)
    }

    override fun subscribe(block: (State) -> Unit): Closeable = state.observeOn(stateDispatcher)
            .subscribe(block)
            .let(::Subscription)

    override fun close() {
        disposable.dispose()
        state.onComplete()
        messages.onComplete()
    }

    private class Subscription(private val disposable: Disposable) : Closeable {
        override fun close() {
            disposable.dispose()
        }
    }
}