package com.securenotes.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.securenotes.app.NotesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reagenda os alarmes de lembretes após a reinicialização do aparelho,
 * pois o Android apaga todos os alarmes ao desligar/ligar.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as NotesApplication
                application.container.reminderRepository.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
