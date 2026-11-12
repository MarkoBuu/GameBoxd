package com.example.ppp.data.local

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/** Runs several database operations atomically. Abstracted so repository logic can be unit-tested. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(private val db: RoomDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
