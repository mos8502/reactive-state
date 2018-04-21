package hu.nemi.costate.arch

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import hu.nemi.store.Observable
import hu.nemi.store.Subscription

interface Block<S> : Observable<S>

interface BlockLifecycleCallback {
    fun onActive() {}
    fun onInactive() {}
    fun onDestroy() {}
}

abstract class BlockViewModel<S, B>(block: B): ViewModel() where B: Block<S>, B: BlockLifecycleCallback {
    private val subscription: Subscription
    private val _state = BlockLiveData<S>(block)
    val state: LiveData<S> = _state

    init {
        subscription = block.subscribe { _state. postValue(it) }
    }

    override fun onCleared() {
        super.onCleared()
        subscription.unsubscribe()
    }
}

private class BlockLiveData<S>(private val target: BlockLifecycleCallback) : MutableLiveData<S>() {
    override fun onActive() {
        super.onActive()
        target.onActive()
    }

    override fun onInactive() {
        super.onInactive()
        target.onInactive()
    }
}

