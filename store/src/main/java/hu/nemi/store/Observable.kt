package hu.nemi.store

interface Subscription {
    fun unsubscribe()

    companion object {
        operator fun invoke(lock: Lock = Lock(), onUnsubscribe: () -> Unit): Subscription =
                DefaultSubscription(lock, onUnsubscribe)
    }
}

interface Observable<out T> {
    fun subscribe(block: (T) -> Unit): Subscription
}

private class DefaultSubscription(private val lock: Lock,
                                  private val onUnsubscribe: () -> Unit) : Subscription {
    @Volatile private var isUnsubscribed = false

    override fun unsubscribe() = lock {
        if (!isUnsubscribed) {
            try {
                onUnsubscribe()
            } finally {
                isUnsubscribed = true
            }
        }
    }
}