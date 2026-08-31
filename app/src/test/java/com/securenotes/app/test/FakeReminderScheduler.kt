package com.securenotes.app.test

import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.reminder.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {

    val scheduled = mutableListOf<Pair<Long, Long>>()
    val cancelled = mutableListOf<Long>()
    val posted = mutableListOf<Reminder>()

    override fun schedule(reminderId: Long, atMillis: Long) {
        scheduled.add(reminderId to atMillis)
    }

    override fun cancel(reminderId: Long) {
        cancelled.add(reminderId)
    }

    override suspend fun postNotification(reminder: Reminder) {
        posted.add(reminder)
    }

    var refreshChannelCalls = 0
    var lastForce: Boolean = false

    override suspend fun refreshChannel(force: Boolean) {
        refreshChannelCalls++
        lastForce = force
    }
}
