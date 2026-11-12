package com.sudharo.provider

import android.app.Application
import com.sudharo.provider.data.local.SudharoDatabase
import com.sudharo.provider.data.remote.ApiClient
import com.sudharo.provider.data.remote.SupabaseAuthService
import com.sudharo.provider.data.repository.SudharoRepository

/**
 * Manual DI holder (no Hilt, to keep the first build reliable in Android Studio).
 * Swap to Hilt later without changing call sites: repositories are behind
 * SudharoRepository, auth behind SupabaseAuthService.
 */
class SudharoApplication : Application() {
    lateinit var database: SudharoDatabase
        private set
    lateinit var repository: SudharoRepository
        private set
    lateinit var authService: SupabaseAuthService
        private set

    override fun onCreate() {
        super.onCreate()
        database = SudharoDatabase.get(this)
        val api = ApiClient.create(BuildConfig.API_BASE_URL)
        authService = SupabaseAuthService(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        repository = SudharoRepository(database, api, authService, this)
    }
}
