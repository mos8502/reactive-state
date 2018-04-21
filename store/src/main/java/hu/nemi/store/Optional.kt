package hu.nemi.store

import java.util.NoSuchElementException

sealed class Optional<out T> {
    object Empty : Optional<Nothing>()
    data class Value<out T>(val value: T) : Optional<T>()

    companion object {
        fun <T> of(value: T?) : Optional<T> = if(value == null) Empty else Value(value)
    }

    inline fun <B> map(f: (T) -> B): Optional<B> = when (this) {
        is Empty -> this
        is Value -> Value(f(value))
    }

    inline fun <B> mapEmpty(f: () -> B): Optional<B> = when (this) {
        is Empty -> Value(f())
        is Value -> Empty
    }

    inline fun <B> flatMap(f: (T) -> Optional<B>): Optional<B> = when (this) {
        is Empty -> this
        is Value -> f(value)
    }

    inline fun getOrThrow() = getOrThrow { NoSuchElementException() }

    inline fun getOrThrow(f: () -> Throwable): T = when (this) {
        is Empty -> throw f()
        is Value -> value
    }

    inline fun <B> zip(other: Optional<B>): Optional<Pair<T, B>> = when (this) {
        is Empty -> Empty
        is Value -> other.flatMap { Value(Pair(value, it)) }
    }
}

inline fun <T> Optional<T>.getOrDefault(default: T): T = when (this) {
    is Optional.Empty -> default
    is Optional.Value -> value
}


fun <T> T?.asOptional() : Optional<T> = Optional.of(this)