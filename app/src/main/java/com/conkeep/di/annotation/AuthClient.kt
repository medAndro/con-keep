package com.conkeep.di.annotation

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainAuthClient
