package hu.nemi.costate.di

import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class StoreDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class BackgroundDispatcher