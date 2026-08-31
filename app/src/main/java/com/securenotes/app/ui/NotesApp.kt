package com.securenotes.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securenotes.app.NotesApplication
import com.securenotes.app.domain.settings.SettingsRepository
import com.securenotes.app.domain.settings.ThemeMode
import com.securenotes.app.security.BiometricAuthenticator
import com.securenotes.app.security.SecurityManager
import com.securenotes.app.ui.navigation.Screen
import com.securenotes.app.ui.screens.editor.EditorRoute
import com.securenotes.app.ui.screens.home.HomeRoute
import com.securenotes.app.ui.screens.reminders.RemindersRoute
import com.securenotes.app.ui.screens.security.LockRoute
import com.securenotes.app.ui.screens.security.SecuritySetupRoute
import com.securenotes.app.ui.screens.settings.SettingsRoute
import com.securenotes.app.ui.screens.trash.TrashRoute
import com.securenotes.app.ui.theme.SecureNotesTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NotesApp(
    pendingNoteId: StateFlow<Long?>? = null,
    onPendingNoteConsumed: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as NotesApplication
    val themeMode by application.container.settingsRepository.themeMode
        .collectAsState(initial = ThemeMode.DARK)
    val customPhotoUri by application.container.settingsRepository.customPhotoUri
        .collectAsState(initial = "")
    SecureNotesTheme(themeMode = themeMode, customPhotoUri = customPhotoUri) {
        val securityManager = application.container.securityManager
        val biometricAuthenticator = application.container.biometricAuthenticator

        val hasPin by securityManager.hasPin.collectAsState(initial = false)
        val authenticated by securityManager.authenticated.collectAsState()

        if (!hasPin) {
            SecuritySetupRoute(
                securityManager = securityManager,
                biometricAuthenticator = biometricAuthenticator,
                onComplete = { securityManager.unlock() }
            )
        } else if (!authenticated) {
            LockRoute(
                securityManager = securityManager,
                biometricAuthenticator = biometricAuthenticator
            )
        } else {
            NotesNavHost(
                securityManager = securityManager,
                biometricAuthenticator = biometricAuthenticator,
                settingsRepository = application.container.settingsRepository,
                reminderScheduler = application.container.reminderScheduler,
                pendingNoteId = pendingNoteId,
                onPendingNoteConsumed = onPendingNoteConsumed
            )
        }
    }
}

@Composable
private fun NotesNavHost(
    securityManager: SecurityManager,
    biometricAuthenticator: BiometricAuthenticator,
    settingsRepository: SettingsRepository,
    reminderScheduler: com.securenotes.app.reminder.ReminderScheduler,
    pendingNoteId: StateFlow<Long?>? = null,
    onPendingNoteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val pendingNote = pendingNoteId?.collectAsState()?.value

    LaunchedEffect(pendingNote) {
        if (pendingNote != null) {
            navController.navigate(Screen.Editor.withArgs(pendingNote!!)) {
                launchSingleTop = true
            }
            onPendingNoteConsumed()
        }
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeRoute(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.withArgs(noteId))
                },
                onNewNote = { folderId ->
                    navController.navigate(Screen.Editor.withArgs(0L, folderId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onOpenFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onOpenFolder = { folderId ->
                    navController.navigate(Screen.Folder.withArgs(folderId))
                },
                onOpenReminders = {
                    navController.navigate(Screen.Reminders.route)
                }
            )
        }
        composable(Screen.Favorites.route) {
            HomeRoute(
                favoritesOnly = true,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.withArgs(noteId))
                },
                onNewNote = { folderId ->
                    navController.navigate(Screen.Editor.withArgs(0L, folderId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onOpenFavorites = {
                    navController.popBackStack(Screen.Favorites.route, inclusive = false)
                },
                onOpenMyNotes = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onOpenFolder = { folderId ->
                    navController.navigate(Screen.Folder.withArgs(folderId))
                },
                onOpenReminders = {
                    navController.navigate(Screen.Reminders.route)
                }
            )
        }
        composable(
            route = Screen.Folder.route,
            arguments = listOf(
                navArgument(Screen.FOLDER_ID_ARG) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong(Screen.FOLDER_ID_ARG) ?: return@composable
            HomeRoute(
                folderId = folderId,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.withArgs(noteId))
                },
                onNewNote = { selectedFolderId ->
                    navController.navigate(Screen.Editor.withArgs(0L, selectedFolderId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onOpenFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onOpenMyNotes = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onOpenFolder = { selectedFolderId ->
                    navController.navigate(Screen.Folder.withArgs(selectedFolderId))
                },
                onOpenReminders = {
                    navController.navigate(Screen.Reminders.route)
                }
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument(Screen.NOTE_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(Screen.FOLDER_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong(Screen.NOTE_ID_ARG) ?: 0L
            val initialFolderId = backStackEntry.arguments
                ?.getLong(Screen.FOLDER_ID_ARG)
                ?.takeUnless { it == -1L }
            EditorRoute(
                noteId = noteId,
                initialFolderId = initialFolderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsRoute(
                securityManager = securityManager,
                biometricAuthenticator = biometricAuthenticator,
                settingsRepository = settingsRepository,
                reminderScheduler = reminderScheduler,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Trash.route) {
            TrashRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reminders.route) {
            RemindersRoute(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.withArgs(noteId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
