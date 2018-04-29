package hu.nemi.store

/**
 * Node reference that allows modification an query of the node and it's value
 */
interface Node<T : Any> {
    val node: Lens<StateNode<Any>, StateNode<T>>
    val value: Lens<StateNode<Any>, T>

    fun <C : Any> withChild(key: Any, init: () -> C): Node<C>

    companion object {
        /**
         * Factory method for constructing root node
         */
        operator fun <T : Any> invoke(): Node<T> = object : Node<T> {
            override val node: Lens<StateNode<Any>, StateNode<T>> = Lens(
                    get = { it as StateNode<T> },
                    set = { child -> { parent -> parent.copy(value = child.value, children = child.children) } }
            )
            override val value: Lens<StateNode<Any>, T> = Lens(
                    get = { it.value as T },
                    set = { value -> { node -> node.copy(value = value) } }
            )

            override fun <C : Any> withChild(key: Any, init: () -> C): Node<C> = DefaultNode(parent = this, key = key, init = init)
        }
    }
}

/**
 * Represents the data of a state node
 */
data class StateNode<V : Any>(val value: V, val children: Map<Any, StateNode<*>>) {
    companion object {
        operator fun invoke(value: Any): StateNode<Any> = StateNode(value = value, children = emptyMap())
    }
}

/**
 * Default implementation of a [Node]
 */
private data class DefaultNode<P : Any, T : Any>(val parent: Node<P>, val key: Any, val init: () -> T) : Node<T> {
    override val node: Lens<StateNode<Any>, StateNode<T>> = parent.node + Lens(
            get = {
                it.children[key] as? StateNode<T>
                        ?: StateNode(value = init(), children = emptyMap())
            },
            set = { child -> { parent -> parent.copy(children = parent.children + (key to child)) } }
    )

    override val value: Lens<StateNode<Any>, T> = node + Lens(
            get = { it.value },
            set = { value -> { node -> node.copy(value = value) } }
    )

    override fun <C : Any> withChild(key: Any, init: () -> C): Node<C> = DefaultNode(parent = this, key = key, init = init)
}
