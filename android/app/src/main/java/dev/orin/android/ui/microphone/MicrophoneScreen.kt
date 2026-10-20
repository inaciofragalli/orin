package dev.orin.android.ui.microphone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.orin.android.service.CaptureStatus

@Composable
fun MicrophoneScreen(
    hasMicrophonePermission: Boolean,
    captureStatus: CaptureStatus,
    errorMessage: String?,
    isCapturing: Boolean,
    requestMicrophonePermission: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
//            val isTransitioning = captureStatus == CaptureStatus.CAPTURING ||
//                captureStatus == CaptureStatus.STOPPING
//
//            Button(
//                enabled = !isTransitioning,
//                onClick = {
//                    if (!hasMicrophonePermission) {
//                        requestMicrophonePermission()
//                    } else {
//                        if (isCapturing) {
//                            onStopCapture()
//                        } else {
//                            onStartCapture()
//                        }
//                    }
//                }
//            ) {
//                Text(if (isCapturing) "Stop" else "Start")
//            }

            if (hasMicrophonePermission) {
                val isTransitioning = captureStatus == CaptureStatus.STARTING ||
                    captureStatus == CaptureStatus.STOPPING

                Button(
                    enabled = !isTransitioning,
                    onClick = {
                        if (isCapturing) {
                            onStopCapture()
                        } else {
                            onStartCapture()
                        }
                    }
                ) {
                    Text(if (isCapturing) "Stop" else "Start")
                }
            } else {
                Button(onClick = requestMicrophonePermission) {
                    Text(if (isCapturing) "Stop" else "Start (Permission required)")
                }
            }

            Text(if (errorMessage != null) "Error: $errorMessage" else "")
        }
    }
}
