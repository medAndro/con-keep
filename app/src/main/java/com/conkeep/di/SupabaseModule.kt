package com.conkeep.di

import android.util.Log
import com.conkeep.BuildConfig
import com.conkeep.data.auth.AuthEventBus
import com.conkeep.data.repository.auth.AuthRepository
import com.conkeep.di.annotation.AuthClient
import com.conkeep.di.annotation.PlainAuthClient
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
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Singleton
import io.ktor.client.plugins.auth.Auth as KtorAuth

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

    @Provides
    @PlainAuthClient
    @Singleton
    fun providePlainAuthClient(): HttpClient =
        HttpClient(Android) {
            // 공통 JSON 설정
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    },
                )
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            }
        }

    @Provides
    @AuthClient
    @Singleton
    fun provideAuthClient(
        authRepository: AuthRepository,
        authEventBus: AuthEventBus,
    ): HttpClient =
        HttpClient(Android) {
            // 공통 JSON 설정
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    },
                )
            }

            // Bearer Token Auth 플러그인
            install(KtorAuth) {
                bearer {
                    cacheTokens = false
                    loadTokens {
                        val accessToken = authRepository.currentAccessToken()
                        if (accessToken != null) {
                            BearerTokens(accessToken, refreshToken = "not_used")
                        } else {
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val apiHost = URL(BuildConfig.BASE_URL).host
                        request.url.host == apiHost
                    }

                    refreshTokens {
                        val newToken = authRepository.refreshAccessToken()
                        if (newToken != null) {
                            BearerTokens(newToken, refreshToken = "not_used")
                        } else {
                            null
                        }
                    }
                }
            }

            // HTTP 응답 검증기 (401 전역 가로채기)
            expectSuccess = true // status code가 200번대가 아니면 예외 발생
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    val clientException =
                        exception as? ClientRequestException
                            ?: return@handleResponseExceptionWithRequest
                    val exceptionResponse = clientException.response

                    // 응답이 401 Unauthorized인 경우
                    if (exceptionResponse.status == HttpStatusCode.Unauthorized) {
                        Log.d("AuthClient", "401 Unauthorized 감지. 전역 로그아웃 처리")
                        authEventBus.emitForceLogout()
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            }
        }
}
