package com.johngabie.johnpdf.ui.reader

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.RotationLockSetting
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.testutil.FakePdfEngine
import com.johngabie.johnpdf.testutil.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeRotation : RotationLockSetting {
        override val rotationLocked = MutableStateFlow(false)
        override suspend fun setRotationLocked(locked: Boolean) { rotationLocked.value = locked }
    }

    private val engine = FakePdfEngine()
    private val rotation = FakeRotation()
    private fun vm() = ReaderViewModel(File("/x.pdf"), "x.pdf", engine, rotation)

    @Test fun opens_ready_with_page_sizes() {
        val s = vm().state.value
        assertEquals("x.pdf", s.title)
        assertEquals(ReaderStatus.Ready(List(3) { PageSize(595f, 842f) }), s.status)
        assertEquals(3, s.pageCount)
    }

    @Test fun password_flow() {
        engine.password = "1234"
        val vm = vm()
        assertEquals(ReaderStatus.NeedsPassword(wrongAttempt = false), vm.state.value.status)
        vm.submitPassword("0000")
        assertEquals(ReaderStatus.NeedsPassword(wrongAttempt = true), vm.state.value.status)
        vm.submitPassword("1234")
        assertTrue(vm.state.value.status is ReaderStatus.Ready)
    }

    @Test fun corrupted_fails() {
        engine.corrupted = true
        assertEquals(ReaderStatus.Failed(AppError.CORRUPTED), vm().state.value.status)
    }

    @Test fun zoom_is_clamped() {
        val vm = vm()
        vm.setZoom(0.3f); assertEquals(1f, vm.state.value.zoom)
        vm.setZoom(10f); assertEquals(4f, vm.state.value.zoom)
    }

    @Test fun double_tap_toggles_between_1_and_2_5() {
        val vm = vm()
        vm.toggleDoubleTapZoom(); assertEquals(2.5f, vm.state.value.zoom)
        vm.toggleDoubleTapZoom(); assertEquals(1f, vm.state.value.zoom)
        vm.setZoom(3.2f); vm.toggleDoubleTapZoom(); assertEquals(1f, vm.state.value.zoom)
    }

    @Test fun page_visible_is_clamped() {
        val vm = vm()
        vm.onPageVisible(1); assertEquals(1, vm.state.value.currentPage)
        vm.onPageVisible(99); assertEquals(2, vm.state.value.currentPage)
    }

    @Test fun render_oom_retries_at_half_width() = runTest {
        engine.renderFailures += OutOfMemoryError("fake")
        val bmp = vm().renderPage(0, 800)
        assertEquals(400, bmp!!.width)
        assertEquals(listOf(0 to 800, 0 to 400), engine.renderRequests)
    }

    @Test fun render_oom_retry_halves_the_clamped_width_not_the_requested_one() = runTest {
        engine.renderFailures += OutOfMemoryError("fake")
        vm().renderPage(0, 5000)
        assertEquals(listOf(0 to 5000, 0 to 1024), engine.renderRequests)
    }

    @Test fun render_oom_twice_returns_null() = runTest {
        engine.renderFailures += OutOfMemoryError("1"); engine.renderFailures += OutOfMemoryError("2")
        assertNull(vm().renderPage(0, 800))
    }

    @Test fun render_runtime_exception_returns_null() = runTest {
        engine.renderFailures += RuntimeException("página quebrada")
        assertNull(vm().renderPage(0, 800))
    }

    @Test fun rotation_lock_toggles_through_setting() {
        val vm = vm()
        vm.toggleRotationLock()
        assertTrue(rotation.rotationLocked.value)
        assertTrue(vm.state.value.rotationLocked)
    }

    @Test fun clearing_view_model_closes_engine() {
        val store = ViewModelStore()
        ViewModelProvider(store, viewModelFactory { initializer { vm() } })[ReaderViewModel::class.java]
        store.clear()
        assertTrue(engine.closed)
    }
}
