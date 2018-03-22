package hu.nemi.costate.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext

@Module
object CoroutinesModule {
    @get:[Provides Main] @JvmStatic
    val mainDispatcher: CoroutineContext = UI

    @get:[Provides Background] @JvmStatic
    val backgroundDispatcher: CoroutineContext = DefaultDispatcher
}