package com.securenotes.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.securenotes.app.security.SecurityManager
import com.securenotes.app.ui.NotesApp
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
        private const val REQ_NOTIFICATION_PERMISSION = 4242
        private const val REQ_RINGTONE_PICKER = 4243
        private const val REQ_MIC_PERMISSION = 4244
        private const val TAG = "MainActivity"

        @Volatile
        private var activeActivity: WeakReference<MainActivity>? = null

        @Volatile
        private var notificationPermissionCallback: ((Boolean) -> Unit)? = null

        @Volatile
        private var ringtonePickerCallback: ((Uri?) -> Unit)? = null

        /** Activity em primeiro plano usada para hospedar o BiometricPrompt. */
        @JvmStatic
        fun currentActivity(): MainActivity? = activeActivity?.get()
    }

    private val _pendingNoteId = MutableStateFlow<Long?>(null)
    val pendingNoteId: StateFlow<Long?> = _pendingNoteId.asStateFlow()

    private val securityManager: SecurityManager
        get() = (application as NotesApplication).container.securityManager

    private var micPermissionCallback: ((Boolean) -> Unit)? = null

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onResult(true)
            return
        }
        notificationPermissionCallback = onResult
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQ_NOTIFICATION_PERMISSION
        )
    }

    fun requestMicrophonePermission(onResult: (Boolean) -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onResult(true)
            return
        }
        micPermissionCallback = onResult
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQ_MIC_PERMISSION
        )
    }

    fun launchRingtonePicker(callback: (Uri?) -> Unit) {
        ringtonePickerCallback = callback
        try {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_custom_sound_pick))
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_RINGTONE_PICKER)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ringtone picker", e)
            ringtonePickerCallback = null
            callback(null)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_RINGTONE_PICKER) {
            val callback = ringtonePickerCallback
            ringtonePickerCallback = null
            if (resultCode == RESULT_OK && data != null) {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                callback?.invoke(uri)
            } else {
                callback?.invoke(null)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFICATION_PERMISSION) {
            notificationPermissionCallback?.invoke(
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            )
            notificationPermissionCallback = null
        }
        if (requestCode == REQ_MIC_PERMISSION) {
            micPermissionCallback?.invoke(
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            )
            micPermissionCallback = null
        }
    }

    init {
        lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    // Bloqueia e remove conteúdo sensível da visão geral de aplicativos.
                    securityManager.lock()
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            NotesApp(
                pendingNoteId = _pendingNoteId,
                onPendingNoteConsumed = { _pendingNoteId.value = null }
            )
        }
        lifecycleScope.launch {
            securityManager.authenticated.collect { authenticated ->
                if (authenticated) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val noteId = intent?.getLongExtra(EXTRA_NOTE_ID, -1L) ?: -1L
        if (noteId > 0L) {
            _pendingNoteId.value = noteId
        }
    }

    override fun onResume() {
        super.onResume()
        activeActivity = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        if (activeActivity?.get() === this) {
            activeActivity = null
        }
    }
}