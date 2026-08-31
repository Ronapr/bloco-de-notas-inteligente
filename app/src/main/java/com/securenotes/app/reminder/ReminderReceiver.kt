package com.securenotes.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.securenotes.app.NotesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AndroidReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as NotesApplication
                application.container.reminderRepository.onReminderFired(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
