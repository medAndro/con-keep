package com.conkeep.di

import com.conkeep.BuildConfig
import com.conkeep.di.annotation.R2UploadClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }

    @Provides
    @R2UploadClient
    @Singleton
    fun provideR2Client(): HttpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(
                    Json {
                        // 정의되지 않은 키가 JSON에 있어도 에러를 내지 않고 무시
                        ignoreUnknownKeys = true
                        // 유연한 파싱을 위해 추가하면 좋은 설정들
                        isLenient = true
                        encodeDefaults = true
                    },
                )
            }
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID // 안드로이드 Logcat에 출력
                    level = LogLevel.HEADERS
                }
            }
        }
}
