package dev.orin.android.service

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CaptureSessionTest {

    @Before
    fun setUp() {
        CaptureSession.update(CaptureStatus.READY)
    }

    @After
    fun tearDown() {
        CaptureSession.update(CaptureStatus.READY)
    }

    @Test
    fun `should update capture status`() {
        CaptureSession.update(CaptureStatus.STARTING)

        assertEquals(
            CaptureStatus.STARTING,
            CaptureSession.state.value.status
        )

        CaptureSession.update(CaptureStatus.CAPTURING)

        assertEquals(
            CaptureStatus.CAPTURING,
            CaptureSession.state.value.status
        )
    }

    @Test
    fun `should store error message`() {
        CaptureSession.update(
            CaptureStatus.ERROR,
            "Microphone unavailable"
        )

        val state = CaptureSession.state.value

        assertEquals(CaptureStatus.ERROR, state.status)
        assertEquals("Microphone unavailable", state.errorMessage)
    }

    @Test
    fun `should clear previous error on status change`() {
        CaptureSession.update(
            CaptureStatus.ERROR,
            "Test error"
        )

        CaptureSession.update(CaptureStatus.STARTING)

        val state = CaptureSession.state.value

        assertEquals(CaptureStatus.STARTING, state.status)
        assertNull(state.errorMessage)
    }

    @Test
    fun `should transition from capturing to stopped`() {
        CaptureSession.update(CaptureStatus.CAPTURING)
        CaptureSession.update(CaptureStatus.STOPPING)
        CaptureSession.update(CaptureStatus.STOPPED)

        assertEquals(
            CaptureStatus.STOPPED,
            CaptureSession.state.value.status
        )
    }
}
