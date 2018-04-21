package hu.nemi.store

interface Subscription {
    fun unsubscribe()
}

interface Observable<T> {
    fun subscribe(block: (T) -> Unit): Subscription
}