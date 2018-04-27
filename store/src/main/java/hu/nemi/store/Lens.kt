package hu.nemi.store

interface Lens<T, V> {

    fun get(t: T): V
    fun set(t: T, v: V): T
}
