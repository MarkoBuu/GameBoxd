package com.example.gameboxd.di

import android.content.Context
import androidx.room.Room
import com.example.gameboxd.data.local.AppDatabase
import com.example.gameboxd.data.local.KeyValueStore
import com.example.gameboxd.data.local.RoomTransactionRunner
import com.example.gameboxd.data.local.SharedPreferencesStore
import com.example.gameboxd.data.local.TransactionRunner
import com.example.gameboxd.data.remote.IgdbApi
import com.example.gameboxd.data.remote.IgdbKtorApi
import com.example.gameboxd.data.repository.GameRepository
import com.example.gameboxd.ui.viewmodel.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { IgdbKtorApi.createHttpClient() }
    single<IgdbApi> { IgdbKtorApi(get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "playstation_rater.db"
        )
            .addMigrations(AppDatabase.MIGRATION_5_6)
            // Only schemas older than v5 (which have no migration path) may be reset.
            // Any newer schema change must ship a real Migration so the library is never wiped.
            .fallbackToDestructiveMigrationFrom(true, *AppDatabase.UNMIGRATABLE_VERSIONS)
            // Downgrades only happen when sideloading an older build during development.
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }
    single { get<AppDatabase>().gameDao() }
    single<KeyValueStore> {
        SharedPreferencesStore(
            androidContext().getSharedPreferences("igdb_migration", Context.MODE_PRIVATE)
        )
    }
    single<TransactionRunner> { RoomTransactionRunner(get<AppDatabase>()) }
    single { GameRepository(get(), get(), get(), get()) }
    single { GameViewModel(get()) }
}
