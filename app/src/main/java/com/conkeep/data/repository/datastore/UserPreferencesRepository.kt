package com.conkeep.data.repository.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val USER_ID = stringPreferencesKey("user_id")
            val FCM_TOKEN = stringPreferencesKey("fcm_token")
            val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        }

        // 데이터 읽기 (Flow) - 데이터가 바뀔 때마다 자동으로 배출됩니다.
        val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
        val fcmToken: Flow<String?> = dataStore.data.map { it[Keys.FCM_TOKEN] }
        val lastSyncTime: Flow<String?> = dataStore.data.map { it[Keys.LAST_SYNC_TIME] }

        // 데이터 저장 (suspend) - 코루틴 안에서 실행되어 UI 스레드를 차단하지 않습니다.
        suspend fun updateUserId(id: String?) = dataStore.edit { it[Keys.USER_ID] = id ?: "" }

        suspend fun updateFcmToken(token: String?) = dataStore.edit { it[Keys.FCM_TOKEN] = token ?: "" }

        suspend fun updateLastSyncTime(time: String) = dataStore.edit { it[Keys.LAST_SYNC_TIME] = time }

        // 로그아웃 시 전체 초기화
        suspend fun clearAll() = dataStore.edit { it.clear() }
    }
