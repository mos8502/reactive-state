package hu.nemi.store

import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicLong

interface Lock {
    operator fun <R> invoke(block: () -> R): R

    companion object {
        operator fun invoke() : Lock = DefaultLock()
    }
}

private class DefaultLock : Lock {
    private val accessingThread = AtomicLong(-1L)
    @Volatile private var accessCount = 0

    override fun <R> invoke(block: () -> R): R {
        val threadId = Thread.currentThread().id
        return if (accessingThread.get() == threadId || accessingThread.compareAndSet(-1L, threadId)) {
            ++accessCount
            try {
                block()
            } finally {
                if (--accessCount == 0) accessingThread.set(-1L)
            }
        } else {
            throw ConcurrentModificationException()
        }
    }
}