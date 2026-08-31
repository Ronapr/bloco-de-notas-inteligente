package com.securenotes.app.ui.navigation

sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data object Favorites : Screen("favorites")

    data object Folder : Screen("folder/{$FOLDER_ID_ARG}") {
        fun withArgs(folderId: Long): String = "folder/$folderId"
    }

    data object Editor : Screen("editor/{$NOTE_ID_ARG}?$FOLDER_ID_ARG={$FOLDER_ID_ARG}") {
        fun withArgs(noteId: Long, folderId: Long? = null): String =
            "editor/$noteId?$FOLDER_ID_ARG=${folderId ?: -1L}"
    }

    data object Settings : Screen("settings")

    data object Trash : Screen("trash")

    data object Reminders : Screen("reminders")

    companion object {
        const val NOTE_ID_ARG = "noteId"
        const val FOLDER_ID_ARG = "folderId"
    }
}
