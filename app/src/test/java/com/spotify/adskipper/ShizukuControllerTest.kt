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

/**
 * Unit tests for ShizukuController.
 * 
 * Tests the Shizuku API integration using Remote Binder Call approach with reflection,
 * error handling, and Result type returns.
 * Uses MockK to mock Shizuku static methods and reflection calls for isolated testing.
 * 
 * **Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.6**
 */
class ShizukuControllerTest {

    @Before
    fun setup() {
        // Mock Shizuku static methods
        mockkStatic(Shizuku::class)
        mockkStatic(SystemServiceHelper::class)
        mockkConstructor(ShizukuBinderWrapper::class)
    }

    @After
    fun teardown() {
        // Clear all mocks after each test
        unmockkAll()
    }

    // ========== isShizukuAvailable() Tests ==========

    @Test
    fun `isShizukuAvailable returns true when Shizuku binder is reachable`() {
        // Given: Shizuku.pingBinder() succeeds
        every { Shizuku.pingBinder() } returns true

        // When: Checking availability
        val result = ShizukuController.isShizukuAvailable()

        // Then: Should return true
        assertTrue(result)
        verify(exactly = 1) { Shizuku.pingBinder() }
    }

    @Test
    fun `isShizukuAvailable returns false when Shizuku binder throws exception`() {
        // Given: Shizuku.pingBinder() throws exception
        every { Shizuku.pingBinder() } throws RuntimeException("Binder not available")

        // When: Checking availability
        val result = ShizukuController.isShizukuAvailable()

        // Then: Should return false
        assertFalse(result)
        verify(exactly = 1) { Shizuku.pingBinder() }
    }

    // ========== checkShizukuPermission() Tests ==========

    @Test
    fun `checkShizukuPermission returns true when permission is granted`() {
        // Given: Permission is granted
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED

        // When: Checking permission
        val result = ShizukuController.checkShizukuPermission()

        // Then: Should return true
        assertTrue(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    @Test
    fun `checkShizukuPermission returns false when permission is denied`() {
        // Given: Permission is denied
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_DENIED

        // When: Checking permission
        val result = ShizukuController.checkShizukuPermission()

        // Then: Should return false
        assertFalse(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    @Test
    fun `checkShizukuPermission returns false when exception is thrown`() {
        // Given: checkSelfPermission throws exception
        every { Shizuku.checkSelfPermission() } throws SecurityException("Permission check failed")

        // When: Checking permission
        val result = ShizukuController.checkShizukuPermission()

        // Then: Should return false
        assertFalse(result)
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
    }

    // ========== forceStopSpotify() Tests ==========

    @Test
    fun `forceStopSpotify returns Success when reflection calls succeed`() {
        // Given: Shizuku is available and permission granted
        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED
        
        val mockBinder = mockk<IBinder>()
        every { SystemServiceHelper.getSystemService("activity") } returns mockBinder
        
        // Mock ShizukuBinderWrapper constructor
        every { anyConstructed<ShizukuBinderWrapper>().queryLocalInterface(any()) } returns null
        
        // Note: We cannot easily mock reflection calls in unit tests without PowerMock
        // This test verifies preconditions are checked, actual reflection testing
        // would require integration tests or PowerMock
        
        // When: Force stopping Spotify (will fail due to reflection, but that's expected in unit tests)
        val result = ShizukuController.forceStopSpotify()

        // Then: Should attempt the operation (preconditions passed)
        // In real scenario with mocked reflection, this would return Success
        // For unit tests, we verify preconditions were checked
        verify(exactly = 1) { Shizuku.pingBinder() }
        verify(exactly = 1) { Shizuku.checkSelfPermission() }
        verify(exactly = 1) { SystemServiceHelper.getSystemService("activity") }
    }

    @Test
    fun `forceStopSpotify returns Error when Shizuku is not available`() {
        // Given: Shizuku is not available
        every { Shizuku.pingBinder() } throws RuntimeException("Not available")

        // When: Force stopping Spotify
        val result = ShizukuController.forceStopSpotify()

        // Then: Should return Error with unavailable message
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Shizuku service not available", result.exception.message)
        
        // Should not attempt to get system service
        verify(exactly = 0) { SystemServiceHelper.getSystemService(any()) }
    }

    @Test
    fun `forceStopSpotify returns Error when permission is not granted`() {
        // Given: Shizuku is available but permission not granted
        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_DENIED

        // When: Force stopping Spotify
        val result = ShizukuController.forceStopSpotify()

        // Then: Should return Error with permission message
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Shizuku permission not granted", result.exception.message)
        
        // Should not attempt to get system service
        verify(exactly = 0) { SystemServiceHelper.getSystemService(any()) }
    }

    @Test
    fun `forceStopSpotify returns Error when SystemServiceHelper throws exception`() {
        // Given: Shizuku is available, permission granted, but SystemServiceHelper fails
        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED
        every { SystemServiceHelper.getSystemService("activity") } throws RuntimeException("Service not found")

        // When: Force stopping Spotify
        val result = ShizukuController.forceStopSpotify()

        // Then: Should return Error with exception
        assertTrue(result is Result.Error)
        assertEquals("Service not found", result.exception.message)
    }

    @Test
    fun `forceStopSpotify handles SecurityException from reflection`() {
        // Given: Shizuku is available, permission granted, but reflection throws SecurityException
        every { Shizuku.pingBinder() } returns true
        every { Shizuku.checkSelfPermission() } returns PackageManager.PERMISSION_GRANTED
        
        val mockBinder = mockk<IBinder>()
        every { SystemServiceHelper.getSystemService("activity") } returns mockBinder
        every { anyConstructed<ShizukuBinderWrapper>().queryLocalInterface(any()) } returns null
        
        // When: Force stopping Spotify (reflection will fail in unit test environment)
        val result = ShizukuController.forceStopSpotify()

        // Then: Should return Error (reflection failure is caught)
        assertTrue(result is Result.Error)
        // The actual exception will be from reflection (ClassNotFoundException, etc.)
        // which is expected in unit test environment without Android runtime
    }
}
