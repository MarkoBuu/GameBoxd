package com.example.ppp.di

import android.content.Context
import androidx.room.Room
import com.example.ppp.data.local.AppDatabase
import com.example.ppp.data.remote.RawgKtorApi
import com.example.ppp.data.repository.GameRepository
import com.example.ppp.ui.viewmodel.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { RawgKtorApi.createHttpClient() }
    single { RawgKtorApi(get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "playstation_rater.db"
        )
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .allowMainThreadQueries()
            .build()
    }
    single { get<AppDatabase>().gameDao() }
    single { androidContext().getSharedPreferences("ppp_prefs", Context.MODE_PRIVATE) }
    single { GameRepository(get(), get(), get()) }
    viewModel { GameViewModel(get()) }
}
