package com.securenotes.app

import android.app.Application
import com.securenotes.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotesApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.reminderScheduler.refreshChannel()
            // Atualizações do app, force-stop e reinício do aparelho apagam os
            // alarmes agendados; reagenda futuros e notifica atrasados.
            container.reminderRepository.rescheduleAll()
        }
    }
}
