package dev.orin.android.ui.microphone

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.orin.android.service.CaptureSession
import dev.orin.android.service.CaptureStatus
import dev.orin.android.service.MicrophoneService
import kotlinx.coroutines.launch

class MicrophoneViewModel(application: Application) : AndroidViewModel(application) {

    var captureStatus by mutableStateOf(CaptureStatus.READY)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val isCapturing: Boolean
        get() = captureStatus == CaptureStatus.CAPTURING

    init {
        viewModelScope.launch {
            CaptureSession.state.collect { snapshot ->
                captureStatus = snapshot.status
                errorMessage = snapshot.errorMessage
            }
        }
    }

    fun startCapture() {
        val context = getApplication<Application>()

        val intent = Intent(context, MicrophoneService::class.java).apply {
            action = MicrophoneService.ACTION_START
        }

        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            CaptureSession.update(
                CaptureStatus.ERROR,
                e.message ?: e.javaClass.simpleName
            )
        }
    }

    fun stopCapture() {
        val context = getApplication<Application>()

        val intent = Intent(context, MicrophoneService::class.java).apply {
            action = MicrophoneService.ACTION_STOP
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            CaptureSession.update(
                CaptureStatus.ERROR,
                e.message ?: e.javaClass.simpleName
            )
        }
    }
}
