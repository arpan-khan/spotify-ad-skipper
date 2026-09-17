package com.spotify.adskipper

import android.content.pm.PackageManager
import android.os.IBinder
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShizukuControllerTest {

    @Before
    fun setup() {

        mockkStatic(Shizuku::class)
        mockkStatic(SystemServiceHelper::class)
        mockkConstructor(ShizukuBinderWrapper::class)
    }

    @After
    fun teardown() {

        unmockkAll()
    }

    @Test
    fun `isShizukuAvailable returns true when Shizuku binder is reachable`() {

        every { Shizuku.pingBinder() } returns true

        val result = ShizukuController.isShizukuAvailable()

        assertTrue(result)
        verify(exactly = 1) { Shizuku.pingBinder() }
    }

    @Test
    fun `isShizukuAvailable returns false when Shizuku binder throws exception`() {

        every { Shizuku.pingBinder() } throws RuntimeException("Binder not available")

        val result = ShizukuController.isShizukuAvailable()

        assertFalse(result)
        verify(exactly = 1) { Shizuku.pingBinder() }
    }

    @Test
    fun `checkShizukuPermission returns true when permission is granted`() {

        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED

        val result = ShizukuController.checkShizukuPermission()

        assertTrue(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    @Test
    fun `checkShizukuPermission returns false when permission is denied`() {

        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_DENIED

        val result = ShizukuController.checkShizukuPermission()

        assertFalse(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    @Test
    fun `checkShizukuPermission returns false when exception is thrown`() {

        every { Shizuku.checkSelfPermission() } throws SecurityException("Permission check failed")

        val result = ShizukuController.checkShizukuPermission()

        assertFalse(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    @Test
    fun `forceStopSpotify returns Success when reflection calls succeed`() {

        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED

        val mockBinder = mockk<IBinder>()
        every { SystemServiceHelper.getSystemService("activity") } returns mockBinder

        every { anyConstructed<ShizukuBinderWrapper>().queryLocalInterface(any()) } returns null

        val result = ShizukuController.forceStopSpotify()

        verify(exactly = 1) { Shizuku.pingBinder() }
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
        verify(exactly = 1) { SystemServiceHelper.getSystemService("activity") }
    }

    @Test
    fun `forceStopSpotify returns Error when Shizuku is not available`() {

        every { Shizuku.pingBinder() } throws RuntimeException("Not available")

        val result = ShizukuController.forceStopSpotify()

        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Shizuku service not available", result.exception.message)

        verify(exactly = 0) { SystemServiceHelper.getSystemService(any()) }
    }

    @Test
    fun `forceStopSpotify returns Error when permission is not granted`() {

        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_DENIED

        val result = ShizukuController.forceStopSpotify()

        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Shizuku permission not granted", result.exception.message)

        verify(exactly = 0) { SystemServiceHelper.getSystemService(any()) }
    }

    @Test
    fun `forceStopSpotify returns Error when SystemServiceHelper throws exception`() {

        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED
        every { SystemServiceHelper.getSystemService("activity") } throws RuntimeException("Service not found")

        val result = ShizukuController.forceStopSpotify()

        assertTrue(result is Result.Error)
        assertEquals("Service not found", result.exception.message)
    }

    @Test
    fun `forceStopSpotify handles SecurityException from reflection`() {

        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED

        val mockBinder = mockk<IBinder>()
        every { SystemServiceHelper.getSystemService("activity") } returns mockBinder
        every { anyConstructed<ShizukuBinderWrapper>().queryLocalInterface(any()) } returns null

        val result = ShizukuController.forceStopSpotify()

        assertTrue(result is Result.Error)

    }
}
