package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.PosRepository

class PollPosApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { PosRepository(database) }
}
