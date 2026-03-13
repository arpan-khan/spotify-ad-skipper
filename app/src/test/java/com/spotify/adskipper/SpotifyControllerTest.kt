package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SpotifyController.
 * 
 * Tests Spotify lifecycle management including installation checks,
 * app relaunching, and media control intent broadcasting.
 * Uses MockK to mock Android Context and PackageManager for isolated testing.
 * 
 * **Validates: Requirements 4.2, 4.3, 4.4, 4.5**
 */
class SpotifyControllerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        every { mockContext.packageManager } returns mockPackageManager
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ========== isSpotifyInstalled() Tests ==========

    @Test
    fun `isSpotifyInstalled returns true when Spotify is installed`() {
        // Given: Spotify package exists
        val mockPackageInfo = mockk<PackageInfo>()
        every { mockPackageManager.getPackageInfo("com.spotify.music", 0) } returns mockPackageInfo

        // When: Checking if Spotify is installed
        val result = SpotifyController.isSpotifyInstalled(mockContext)

        // Then: Should return true
        assertTrue(result)
        verify(exactly = 1) { mockPackageManager.getPackageInfo("com.spotify.music", 0) }
    }

    @Test
    fun `isSpotifyInstalled returns false when Spotify is not installed`() {
        // Given: Spotify package does not exist
        every { mockPackageManager.getPackageInfo("com.spotify.music", 0) } throws 
            PackageManager.NameNotFoundException()

        // When: Checking if Spotify is installed
        val result = SpotifyController.isSpotifyInstalled(mockContext)

        // Then: Should return false
        assertFalse(result)
        verify(exactly = 1) { mockPackageManager.getPackageInfo("com.spotify.music", 0) }
    }

    // ========== relaunchSpotify() Tests ==========

    @Test
    fun `relaunchSpotify returns Success when Spotify is installed and intent resolves`() {
        // Given: Spotify is installed and launch intent is available
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } just Runs

        // When: Relaunching Spotify
        val result = SpotifyController.relaunchSpotify(mockContext)

        // Then: Should return Success
        assertTrue(result is Result.Success)
        verify(exactly = 1) { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") }
        verify(exactly = 1) { mockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        verify(exactly = 1) { mockContext.startActivity(mockIntent) }
    }

    @Test
    fun `relaunchSpotify returns Error when Spotify is not installed`() {
        // Given: Spotify is not installed (launch intent is null)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns null

        // When: Relaunching Spotify
        val result = SpotifyController.relaunchSpotify(mockContext)

        // Then: Should return Error with appropriate message
        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Spotify not installed", result.exception.message)
        verify(exactly = 1) { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") }
        verify(exactly = 0) { mockContext.startActivity(any()) }
    }

    @Test
    fun `relaunchSpotify returns Error when startActivity throws exception`() {
        // Given: Spotify is installed but startActivity fails
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } throws SecurityException("Permission denied")

        // When: Relaunching Spotify
        val result = SpotifyController.relaunchSpotify(mockContext)

        // Then: Should return Error with exception
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Permission denied", result.exception.message)
    }

    @Test
    fun `relaunchSpotify adds FLAG_ACTIVITY_NEW_TASK to intent`() {
        // Given: Spotify is installed
        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } just Runs

        // When: Relaunching Spotify
        SpotifyController.relaunchSpotify(mockContext)

        // Then: Should add FLAG_ACTIVITY_NEW_TASK flag
        verify(exactly = 1) { mockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    // ========== skipToNext() Tests ==========

    @Test
    fun `skipToNext returns Success when broadcast is sent successfully`() {
        // Given: Context can send broadcasts
        every { mockContext.sendBroadcast(any()) } just Runs

        // When: Sending skip intent
        val result = SpotifyController.skipToNext(mockContext)

        // Then: Should return Success
        assertTrue(result is Result.Success)
        verify(exactly = 1) { mockContext.sendBroadcast(any()) }
    }

    @Test
    fun `skipToNext creates intent with correct action`() {
        // Given: Context can send broadcasts
        val intentSlot = slot<Intent>()
        every { mockContext.sendBroadcast(capture(intentSlot)) } just Runs

        // When: Sending skip intent
        SpotifyController.skipToNext(mockContext)

        // Then: Intent should have correct action
        val capturedIntent = intentSlot.captured
        assertEquals("com.spotify.mobile.android.ui.widget.NEXT", capturedIntent.action)
    }

    @Test
    fun `skipToNext scopes intent to Spotify package`() {
        // Given: Context can send broadcasts
        val intentSlot = slot<Intent>()
        every { mockContext.sendBroadcast(capture(intentSlot)) } just Runs

        // When: Sending skip intent
        SpotifyController.skipToNext(mockContext)

        // Then: Intent should be scoped to Spotify package
        val capturedIntent = intentSlot.captured
        assertEquals("com.spotify.music", capturedIntent.`package`)
    }

    @Test
    fun `skipToNext returns Error when sendBroadcast throws exception`() {
        // Given: sendBroadcast fails
        every { mockContext.sendBroadcast(any()) } throws SecurityException("Broadcast permission denied")

        // When: Sending skip intent
        val result = SpotifyController.skipToNext(mockContext)

        // Then: Should return Error with exception
        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Broadcast permission denied", result.exception.message)
    }

    @Test
    fun `skipToNext handles generic exceptions gracefully`() {
        // Given: sendBroadcast throws unexpected exception
        every { mockContext.sendBroadcast(any()) } throws RuntimeException("Unexpected error")

        // When: Sending skip intent
        val result = SpotifyController.skipToNext(mockContext)

        // Then: Should return Error with exception
        assertTrue(result is Result.Error)
        assertTrue(result.exception is RuntimeException)
        assertEquals("Unexpected error", result.exception.message)
    }
}
