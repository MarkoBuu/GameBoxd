package com.example.ppp.di

import com.example.ppp.data.local.LocalGameDbHelper
import com.example.ppp.data.remote.RawgKtorApi
import com.example.ppp.data.repository.GameRepository
import com.example.ppp.ui.viewmodel.GameViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { RawgKtorApi.createHttpClient() }
    single { RawgKtorApi(get()) }
    single { LocalGameDbHelper(get()) }
    single { GameRepository(get(), get()) }
    viewModel { GameViewModel(get()) }
}
