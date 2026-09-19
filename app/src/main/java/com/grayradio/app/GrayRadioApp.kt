package com.grayradio.app

import android.app.Application
import com.grayradio.app.data.db.AppDatabase
import com.grayradio.app.data.repository.StationRepository
import com.grayradio.app.player.RadioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GrayRadioApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var repository: StationRepository
        private set
    lateinit var radioPlayer: RadioPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        repository = StationRepository(database.stationDao())
        radioPlayer = RadioPlayer(this)
        appScope.launch {
            repository.ensureSeeded()
        }
    }
}
