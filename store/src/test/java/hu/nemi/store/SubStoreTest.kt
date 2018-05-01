package hu.nemi.store

import com.nhaarman.mockito_kotlin.*
import org.junit.Test
import java.util.Base64

class SubStoreTest {
    sealed class Action {
        data class SetUsername(val username: String): Action()
        data class SetPassword(val password: String): Action()
    }

    data class SetName(val name: String)

    private val lens = Lens<Int, String>(
            get = { it.toString() },
            set = { string: String -> { string.toInt() } }
    )

    @Test
    fun `sub store emits state when changed`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch { it * 2 }
        subStore.dispatch { "13" }

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(46)
            verify(subStoreSubscriber).invoke("46")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
        }
    }

    @Test
    fun `can unsubscribe from sub store`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        val subStoreSubscription = subStore.subscribe(subStoreSubscriber)
        subStoreSubscription.unsubscribe()
        subStore.dispatch { "13" }

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to sub store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        subStore.dispatch(object : ActionCreator<String, (String) -> String> {
            override fun invoke(state: String): ((String) -> String)? = { "13" }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `action creator dispatched to store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch(object : ActionCreator<Int, (Int) -> Int> {
            override fun invoke(state: Int): ((Int) -> Int)? = { 13 }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `actions dispatched by async action creator to sub store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        subStore.dispatch(object : AsyncActionCreator<(String), (String) -> String> {
            override fun invoke(state: String, dispatcher: (ActionCreator<String, (String) -> String>) -> Unit) {
                dispatcher(object : ActionCreator<String, (String) -> String> {
                    override fun invoke(state: String): ((String) -> String)? = { "13" }
                })

                dispatcher(object : ActionCreator<String, (String) -> String> {
                    override fun invoke(state: String): ((String) -> String)? = { "2" }
                })
            }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verify(storeSubscriber).invoke(2)
            verify(subStoreSubscriber).invoke("2")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `actions dispatched by async action creator to store changes state`() {
        val storeSubscriber: (Int) -> Unit = mock()
        val store = StateStore(23)
        val subStoreSubscriber: (String) -> Unit = mock()
        val subStore = store.map(lens)

        store.subscribe(storeSubscriber)
        subStore.subscribe(subStoreSubscriber)
        store.dispatch(object : AsyncActionCreator<(Int), (Int) -> Int> {
            override fun invoke(state: Int, dispatcher: (ActionCreator<Int, (Int) -> Int>) -> Unit) {
                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { 13 }
                })

                dispatcher(object : ActionCreator<Int, (Int) -> Int> {
                    override fun invoke(state: Int): ((Int) -> Int)? = { 2 }
                })
            }
        })

        inOrder(storeSubscriber, subStoreSubscriber) {
            verify(storeSubscriber).invoke(23)
            verify(subStoreSubscriber).invoke("23")
            verify(storeSubscriber).invoke(13)
            verify(subStoreSubscriber).invoke("13")
            verify(storeSubscriber).invoke(2)
            verify(subStoreSubscriber).invoke("2")
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `mapped reducer store states`() {

        data class Credentials(val username: String, val password: String)
        data class User(val name: String, val credentials: String)

        val rootStore = StateStore(Credentials(username = "", password = ""))
        val basicAuthStore = rootStore.map(Lens(
                get = { credentials ->
                    val value = Base64.getEncoder().encodeToString("${credentials.username}:${credentials.password}".toByteArray())
                    "Authorization: Basic $value"
                },
                set = { header: String ->
                    { _: Credentials ->
                        val parts = String(Base64.getDecoder().decode(header.split(' ')[2])).split(':')
                        Credentials(username = parts[0], password = parts[1])
                    }
                }
        ))
        val userReducer: (User, SetName) -> User = { user, setName -> user.copy(name = setName.name) }
        val userStore = basicAuthStore.subState("user") { "Player unknown" }
                .map(Lens(
                        get = { User(name = it.state, credentials = it.parentState) },
                        set = { user: User -> { state: State<String, String> -> state.copy(parentState = user.credentials, state = user.name) } }
                ))
                .withReducer(userReducer)

        val reducer: (Credentials, Action) -> Credentials = { state, action ->
            when(action) {
                is Action.SetUsername -> state.copy(username = action.username)
                is Action.SetPassword -> state.copy(password = action.password)
            }
        }
        val reducerStore = rootStore.withReducer(reducer)

        val rootSubscriber: (Credentials) -> Unit = mock()
        val authSubscriber: (String) -> Unit = mock()
        val userSubscriber: (User) -> Unit = mock()

        rootStore.subscribe(rootSubscriber)
        basicAuthStore.subscribe(authSubscriber)
        userStore.subscribe(userSubscriber)

        rootStore.dispatch { Credentials(username = "usr", password = "pwd") }
        verify(authSubscriber)("Authorization: Basic dXNyOnB3ZA==")
        verify(rootSubscriber)(Credentials(username = "usr", password = "pwd"))
        verify(userSubscriber)(User(credentials = "Authorization: Basic dXNyOnB3ZA==", name = "Player unknown"))
        reset(authSubscriber, rootSubscriber, userSubscriber)

        basicAuthStore.dispatch { "Authorization: Basic aGVsbG86c2F5c21l" }
        verify(rootSubscriber)(Credentials(username = "hello", password = "saysme"))
        verify(authSubscriber)("Authorization: Basic aGVsbG86c2F5c21l")
        verify(userSubscriber)(User(credentials = "Authorization: Basic aGVsbG86c2F5c21l", name = "Player unknown"))
        reset(authSubscriber, rootSubscriber, userSubscriber)

        reducerStore.dispatch(Action.SetUsername("me"))
        verify(rootSubscriber)(Credentials(username = "me", password = "saysme"))
        verify(authSubscriber)("Authorization: Basic bWU6c2F5c21l")
        verify(userSubscriber)(User(credentials = "Authorization: Basic bWU6c2F5c21l", name = "Player unknown"))
        reset(authSubscriber, rootSubscriber, userSubscriber)

        reducerStore.dispatch(Action.SetPassword("despicable"))
        verify(rootSubscriber)(Credentials(username = "me", password = "despicable"))
        verify(authSubscriber)("Authorization: Basic bWU6ZGVzcGljYWJsZQ==")
        verify(userSubscriber)(User(credentials = "Authorization: Basic bWU6ZGVzcGljYWJsZQ==", name = "Player unknown"))
        reset(authSubscriber, rootSubscriber, userSubscriber)

        userStore.dispatch(SetName(name = "Alan Turing"))
        verify(rootSubscriber)(Credentials(username = "me", password = "despicable"))
        verify(authSubscriber)("Authorization: Basic bWU6ZGVzcGljYWJsZQ==")
        verify(userSubscriber)(User(credentials = "Authorization: Basic bWU6ZGVzcGljYWJsZQ==", name = "Alan Turing"))
    }
}