package dev.orin.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import dev.orin.android.ui.theme.OrinTheme
import androidx.compose.runtime.getValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import dev.orin.android.audio.MicrophoneCapture
import dev.orin.android.ui.microphone.MicrophoneScreen
import dev.orin.android.ui.microphone.MicrophoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val microphoneViewModel =
            ViewModelProvider(this)[MicrophoneViewModel::class.java]

        setContent {
            var hasMicrophonePermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
                isGranted -> hasMicrophonePermission = isGranted
            }

            OrinTheme {
                MicrophoneScreen(
                    hasMicrophonePermission = hasMicrophonePermission,
                    captureStatus = microphoneViewModel.captureStatus,
                    errorMessage = microphoneViewModel.errorMessage,
                    isCapturing = microphoneViewModel.isCapturing,
                    requestMicrophonePermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartCapture = microphoneViewModel::startCapture,
                    onStopCapture = microphoneViewModel::stopCapture
                )
            }
        }
    }
}
