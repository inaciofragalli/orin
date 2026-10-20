package dev.orin.android.audio

import android.Manifest
import android.annotation.SuppressLint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MicrophoneCaptureTest {

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO
    )

    @SuppressLint("MissingPermission")
    @Test
    fun shouldCaptureCompleteFrame() {
        val capture = MicrophoneCapture()

        try {
            capture.prepare()
            capture.start()

            val frame = capture.readFrame()

            assertEquals(960, frame.size)
        } finally {
            capture.release()
        }
    }

    @SuppressLint("MissingPermission")
    @Test
    fun shouldCaptureMultipleFrames() {
        val capture = MicrophoneCapture()

        try {
            capture.prepare()
            capture.start()

            repeat(100) {
                val frame = capture.readFrame()
                assertEquals(960, frame.size)
            }
        } finally {
            capture.release()
        }
    }

    @SuppressLint("MissingPermission")
    @Test
    fun shouldReleaseResourcesMultipleTimes() {
        val capture = MicrophoneCapture()

        capture.prepare()
        capture.start()
        capture.stop()
        capture.release()

        // release() deve ser idempotente.
        capture.release()
    }
}
