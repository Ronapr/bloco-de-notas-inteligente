package com.securenotes.app.reminder

import com.securenotes.app.domain.model.Reminder

interface ReminderScheduler {

    fun schedule(reminderId: Long, atMillis: Long)

    fun cancel(reminderId: Long)

    suspend fun postNotification(reminder: Reminder)

    /**
     * Cria o canal de notificações com o som e a vibração configurados,
     * se ele ainda não existir ou se as configurações tiverem mudado.
     * [force] recria o canal mesmo que ele já esteja de acordo.
     */
    suspend fun refreshChannel(force: Boolean = false)
}
