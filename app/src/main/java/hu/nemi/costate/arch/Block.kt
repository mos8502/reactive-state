package hu.nemi.costate.arch

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import hu.nemi.store.Observable
import hu.nemi.store.Subscription

interface Block<S> : Observable<S> {
    fun onActive() {}
    fun onInactive() {}
    fun onCleared() {}
}

abstract class BlockViewModel<S, B : Block<S>>(private val block: B) : ViewModel() {
    val state: LiveData<S> = BlockLiveData(block)

    final override fun onCleared() {
        block.onCleared()
    }
}

private class BlockLiveData<S>(private val block: Block<S>) : MutableLiveData<S>() {
    private lateinit var subscription: Subscription

    override fun onActive() {
        block.onActive()
        subscription = block.subscribe(::postValue)
    }

    override fun onInactive() {
        block.onInactive()
        subscription.unsubscribe()
    }
}

