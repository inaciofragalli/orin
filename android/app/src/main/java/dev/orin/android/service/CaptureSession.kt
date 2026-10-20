package dev.orin.android.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CaptureSnapshot(
    val status: CaptureStatus = CaptureStatus.READY,
    val errorMessage: String? = null
)

object CaptureSession {
    private val _state = MutableStateFlow(CaptureSnapshot())

    val state = _state.asStateFlow()

    internal fun update(
        status: CaptureStatus,
        errorMessage: String? = null
    ) {
        _state.value = CaptureSnapshot(status, errorMessage)
    }
}
