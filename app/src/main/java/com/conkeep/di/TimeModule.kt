package com.conkeep.di

import com.conkeep.util.DefaultTimeProvider
import com.conkeep.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: DefaultTimeProvider): TimeProvider
}
