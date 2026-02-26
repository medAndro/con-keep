package com.conkeep.di

import android.content.Context
import coil3.ImageLoader
import coil3.imageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader {
        // context.imageLoader는 Coil에서 제공하는 확장 프로퍼티입니다.
        // SingletonImageLoader.Factory를 구현했다면 그 설정이 적용된 인스턴스를 반환합니다.
        return context.imageLoader
    }
}
