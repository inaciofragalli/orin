package dev.orin.android.service

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dev.orin.android.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MicrophoneServiceTest {

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO
    )

    @Test
    fun shouldStartAndStopMicrophoneService() = runBlocking {
        val context = ApplicationProvider
            .getApplicationContext<Context>()

        CaptureSession.update(CaptureStatus.READY)

        // Mantém a Activity visível ao iniciar o serviço.
        ActivityScenario.launch(MainActivity::class.java).use {
            val startIntent = Intent(
                context,
                MicrophoneService::class.java
            ).apply {
                action = MicrophoneService.ACTION_START
            }

            val stopIntent = Intent(
                context,
                MicrophoneService::class.java
            ).apply {
                action = MicrophoneService.ACTION_STOP
            }

            try {
                ContextCompat.startForegroundService(
                    context,
                    startIntent
                )

                val started = withTimeout(10_000) {
                    CaptureSession.state.first { state ->
                        state.status == CaptureStatus.CAPTURING ||
                            state.status == CaptureStatus.ERROR
                    }
                }

                assertEquals(
                    "Service failed: ${started.errorMessage}",
                    CaptureStatus.CAPTURING,
                    started.status
                )

                context.startService(stopIntent)

                val stopped = withTimeout(10_000) {
                    CaptureSession.state.first { state ->
                        state.status == CaptureStatus.STOPPED ||
                            state.status == CaptureStatus.ERROR
                    }
                }

                assertEquals(
                    "Service failed: ${stopped.errorMessage}",
                    CaptureStatus.STOPPED,
                    stopped.status
                )
            } finally {
                // Solicita encerramento também em caso de falha.
                context.startService(stopIntent)
            }
        }
    }
}
