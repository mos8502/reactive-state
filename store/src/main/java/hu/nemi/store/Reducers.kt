package hu.nemi.store

fun <StateType, ActionType> fold(vararg reducers: (StateType, ActionType) -> StateType): (StateType, ActionType) -> StateType {
    require(reducers.isNotEmpty()) { "no reducers passed" }
    return { state, action ->
        reducers.fold(state) { state, reducer ->
            reducer.invoke(state, action)
        }
    }
}