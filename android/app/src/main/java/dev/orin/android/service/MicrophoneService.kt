package dev.orin.android.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.orin.android.audio.MicrophoneCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MicrophoneService : Service() {

    companion object {
        const val ACTION_START = "dev.orin.android.START_CAPTURE"
        const val ACTION_STOP = "dev.orin.android.STOP_CAPTURE"

        private const val CHANNEL_ID = "orin_microphone"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "OrinMicrophone"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (captureJob == null) {
                    val permissionGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!permissionGranted) {
                        CaptureSession.update(
                            CaptureStatus.ERROR,
                            "Microphone permission not granted"
                        )

                        stopSelf()
                        return START_NOT_STICKY
                    }

                    CaptureSession.update(CaptureStatus.STARTING)

                    try {
                        ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            createNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        )
                    } catch (e: Exception) {
                        CaptureSession.update(
                            CaptureStatus.ERROR,
                            e.message ?: e.javaClass.simpleName
                        )

                        Log.e(TAG, "Cannot start foreground service", e)
                        stopSelf()
                        return START_NOT_STICKY
                    }

                    captureJob = serviceScope.launch {
                        try {
                            val capture = MicrophoneCapture()

                            try {
                                capture.prepare()
                                capture.start()

                                currentCoroutineContext().ensureActive()
                                CaptureSession.update(CaptureStatus.CAPTURING)

                                Log.i(TAG, "Capture started")

                                while (currentCoroutineContext().isActive) {
                                    capture.readFrame()
                                }
                            } finally {
                                capture.release()
                            }
                        } catch (_: CancellationException) {
                            Log.i(TAG, "Capture cancelled")
                        } catch (e: Exception) {
                            CaptureSession.update(
                                CaptureStatus.ERROR,
                                e.message ?: e.javaClass.simpleName
                            )

                            Log.e(TAG, "Capture failed", e)
                        } finally {
                            withContext(NonCancellable + Dispatchers.Main) {
                                if (CaptureSession.state.value.status != CaptureStatus.ERROR) {
                                    CaptureSession.update(CaptureStatus.STOPPED)
                                }

                                Log.i(TAG, "Capture finished")
                                stopSelf()
                            }
                        }
                    }
                }
            }

            ACTION_STOP -> {
                if (captureJob == null) {
                    CaptureSession.update(CaptureStatus.STOPPED)
                    stopSelf()
                } else {
                    CaptureSession.update(CaptureStatus.STOPPING)
                    captureJob?.cancel()
                }
            }

            else -> stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Microphone capture",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, MicrophoneService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Orin")
            .setContentText("Microphone is active")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
}
