package hu.nemi.store

interface Lens<S, A> {
    operator fun invoke(s: S): A
    operator fun invoke(s: S, a: A): S

    operator fun <V> plus(other: Lens<A, V>): Lens<S, V>

    fun modify(s: S, f: (A) -> A): S

    infix fun S.with(a: A) = invoke(this, a)

    companion object {
        operator fun <S, A> invoke(get: (S) -> A, set: (A) -> (S) -> S): Lens<S, A> = DefaultLens(get = get, set = set)
    }
}

/**
 * Lens implementation inspired by the API from Arrow
 */
private class DefaultLens<S, A>(val get: (S) -> A, val set: (A) -> (S) -> S) : Lens<S, A> {
    override fun invoke(s: S): A = get(s)

    override fun invoke(s: S, a: A): S = set(a)(s)

    override fun <V> plus(other: Lens<A, V>): Lens<S, V> = Lens(get = { other(this(it)) }, set = { v: V -> { s: S -> this(s, other(this(s), v)) } })

    override fun modify(s: S, f: (A) -> A): S = invoke(s, f(invoke(s)))
}
