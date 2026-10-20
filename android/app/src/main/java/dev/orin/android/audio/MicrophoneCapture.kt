package dev.orin.android.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission

class MicrophoneCapture {
    companion object {
        private const val SAMPLE_RATE = 48_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val PCM_FRAME_SIZE = 960
    }

    private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)

    private var audioRecord: AudioRecord? = null

    init {
        check(minBufferSize > 0) {
            "Invalid auto buffer size: $minBufferSize"
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun prepare() {
        check(audioRecord == null) {
            "Microphone already initialized"
        }

        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .setEncoding(AUDIO_ENCODING)
            .build()

        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferSize)
            .build()

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("Failed to initialize microphone")
        }

        audioRecord = record
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        val recorder = checkNotNull(audioRecord) {
            "Microphone not initialized"
        }

        recorder.startRecording()

        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
    }

    fun stop() {
        val recorder = checkNotNull(audioRecord) {
            "Microphone not initialized"
        }

        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)

        recorder.stop()
    }

    fun release() {
        val recorder = audioRecord ?: return

        try {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
        } finally {
            recorder.release()
            audioRecord = null
        }
    }

    fun readFrame(): ByteArray {
        val recorder = checkNotNull(audioRecord) {
            "Microphone not initialized"
        }

        val buffer = ByteArray(PCM_FRAME_SIZE)
        var offset = 0

        while (offset < buffer.size) {
            val bytesRead = recorder.read(buffer, offset, buffer.size - offset, AudioRecord.READ_BLOCKING)

            check(bytesRead > 0) {
                "Failed to read audio: $bytesRead"
            }

            offset += bytesRead
        }

        return buffer
    }
}
