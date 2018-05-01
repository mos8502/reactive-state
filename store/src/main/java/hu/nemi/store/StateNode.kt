package hu.nemi.store

data class State<out P : Any, out S : Any>(val parentState: P, val state: S)

internal data class StateNode<out V : Any>(val value: V, val children: Map<Any, StateNode<*>> = emptyMap())

internal interface StateNodeRef<R : Any, V : Any> {
    val node: Lens<StateNode<R>, StateNode<V>>

    val value: Lens<StateNode<R>, V>
        get() = node + Lens(
                get = { it.value },
                set = { value -> { node -> node.copy(value = value) } }
        )

    fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, C>

    companion object {
        operator fun <R : Any> invoke(): StateNodeRef<R, R> = RootStateNodeRef()
    }
}

private class RootStateNodeRef<R : Any> : StateNodeRef<R, R> {
    override val node: Lens<StateNode<R>, StateNode<R>> = Lens(
            get = { it },
            set = { newNode -> { _ -> newNode } }
    )

    override fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, C> =
            ChildStateNodeRef(key = key, parent = this, init = init)
}

private class ChildStateNodeRef<R : Any, V : Any, P : Any>(val key: Any, val parent: StateNodeRef<R, P>, val init: () -> V) : StateNodeRef<R, V> {
    override val node: Lens<StateNode<R>, StateNode<V>> = parent.node + Lens(
            get = { it.children[key] as? StateNode<V> ?: StateNode(value = init()) },
            set = { node -> { parent -> parent.copy(children = parent.children + (key to node)) } }
    )

    override fun <C : Any> addChild(key: Any, init: () -> C): StateNodeRef<R, C> =
            ChildStateNodeRef(key = key, parent = this, init = init)
}

