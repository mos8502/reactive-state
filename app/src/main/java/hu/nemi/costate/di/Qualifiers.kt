package hu.nemi.costate.di

import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class Main

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class Background