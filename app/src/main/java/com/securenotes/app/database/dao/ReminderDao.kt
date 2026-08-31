package com.securenotes.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.securenotes.app.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query(
        "SELECT r.id AS id, r.noteId AS noteId, r.reminderAt AS reminderAt, " +
            "r.shown AS shown, r.createdAt AS createdAt, n.title AS noteTitle " +
            "FROM reminders r JOIN notes n ON n.id = r.noteId " +
            "ORDER BY r.shown ASC, r.reminderAt ASC"
    )
    fun observeReminders(): Flow<List<ReminderWithNoteEntity>>

    @Query(
        "SELECT r.id AS id, r.noteId AS noteId, r.reminderAt AS reminderAt, " +
            "r.shown AS shown, r.createdAt AS createdAt, n.title AS noteTitle " +
            "FROM reminders r JOIN notes n ON n.id = r.noteId " +
            "WHERE r.noteId = :noteId ORDER BY r.reminderAt ASC"
    )
    fun observeRemindersForNote(noteId: Long): Flow<List<ReminderWithNoteEntity>>

    @Query(
        "SELECT r.id AS id, r.noteId AS noteId, r.reminderAt AS reminderAt, " +
            "r.shown AS shown, r.createdAt AS createdAt, n.title AS noteTitle " +
            "FROM reminders r JOIN notes n ON n.id = r.noteId WHERE r.id = :id LIMIT 1"
    )
    suspend fun findById(id: Long): ReminderWithNoteEntity?

    @Query(
        "SELECT r.id AS id, r.noteId AS noteId, r.reminderAt AS reminderAt, " +
            "r.shown AS shown, r.createdAt AS createdAt, n.title AS noteTitle " +
            "FROM reminders r JOIN notes n ON n.id = r.noteId " +
            "WHERE r.reminderAt <= :now AND r.shown = 0"
    )
    suspend fun findOverdueUnshown(now: Long): List<ReminderWithNoteEntity>

    @Query("SELECT noteId, MIN(reminderAt) AS reminderAt FROM reminders WHERE shown = 0 GROUP BY noteId")
    fun observeNextReminderByNote(): Flow<List<NoteReminderAt>>

    @Query(
        "SELECT r.id AS id, r.noteId AS noteId, r.reminderAt AS reminderAt, " +
            "r.shown AS shown, r.createdAt AS createdAt, n.title AS noteTitle " +
            "FROM reminders r JOIN notes n ON n.id = r.noteId " +
            "WHERE r.shown = 0"
    )
    suspend fun findUnshown(): List<ReminderWithNoteEntity>

    @Query("UPDATE reminders SET shown = 1 WHERE id = :id")
    suspend fun markShown(id: Long)

    @Query("UPDATE reminders SET reminderAt = :reminderAt WHERE id = :id")
    suspend fun updateTime(id: Long, reminderAt: Long)

    @Query("SELECT id FROM reminders WHERE noteId = :noteId")
    suspend fun idsByNote(noteId: Long): List<Long>

    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
