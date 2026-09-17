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

    @Test
    fun `isSpotifyInstalled returns true when Spotify is installed`() {

        val mockPackageInfo = mockk<PackageInfo>()
        every { mockPackageManager.getPackageInfo("com.spotify.music", 0) } returns mockPackageInfo

        val result = SpotifyController.isSpotifyInstalled(mockContext)

        assertTrue(result)
        verify(exactly = 1) { mockPackageManager.getPackageInfo("com.spotify.music", 0) }
    }

    @Test
    fun `isSpotifyInstalled returns false when Spotify is not installed`() {

        every { mockPackageManager.getPackageInfo("com.spotify.music", 0) } throws
            PackageManager.NameNotFoundException()

        val result = SpotifyController.isSpotifyInstalled(mockContext)

        assertFalse(result)
        verify(exactly = 1) { mockPackageManager.getPackageInfo("com.spotify.music", 0) }
    }

    @Test
    fun `relaunchSpotify returns Success when Spotify is installed and intent resolves`() {

        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } just Runs

        val result = SpotifyController.relaunchSpotify(mockContext)

        assertTrue(result is Result.Success)
        verify(exactly = 1) { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") }
        verify(exactly = 1) { mockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        verify(exactly = 1) { mockContext.startActivity(mockIntent) }
    }

    @Test
    fun `relaunchSpotify returns Error when Spotify is not installed`() {

        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns null

        val result = SpotifyController.relaunchSpotify(mockContext)

        assertTrue(result is Result.Error)
        assertTrue(result.exception is IllegalStateException)
        assertEquals("Spotify not installed", result.exception.message)
        verify(exactly = 1) { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") }
        verify(exactly = 0) { mockContext.startActivity(any()) }
    }

    @Test
    fun `relaunchSpotify returns Error when startActivity throws exception`() {

        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } throws SecurityException("Permission denied")

        val result = SpotifyController.relaunchSpotify(mockContext)

        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Permission denied", result.exception.message)
    }

    @Test
    fun `relaunchSpotify adds FLAG_ACTIVITY_NEW_TASK to intent`() {

        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } just Runs

        SpotifyController.relaunchSpotify(mockContext)

        verify(exactly = 1) { mockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    @Test
    fun `play returns Success when broadcasts are sent successfully`() {

        every { mockContext.sendBroadcast(any()) } just Runs

        val result = SpotifyController.play(mockContext)

        assertTrue(result is Result.Success)
        verify(exactly = 2) { mockContext.sendBroadcast(any()) }
    }

    @Test
    fun `play sends media button events scoped to Spotify package`() {

        every { mockContext.sendBroadcast(any()) } just Runs

        val result = SpotifyController.play(mockContext)

        assertTrue(result is Result.Success)
        verify(exactly = 2) { mockContext.sendBroadcast(any()) }
    }

    @Test
    fun `play returns Error when sendBroadcast throws exception`() {

        every { mockContext.sendBroadcast(any()) } throws SecurityException("Broadcast permission denied")

        val result = SpotifyController.play(mockContext)

        assertTrue(result is Result.Error)
        assertTrue(result.exception is SecurityException)
        assertEquals("Broadcast permission denied", result.exception.message)
    }

    @Test
    fun `play handles generic exceptions gracefully`() {

        every { mockContext.sendBroadcast(any()) } throws RuntimeException("Unexpected error")

        val result = SpotifyController.play(mockContext)

        assertTrue(result is Result.Error)
        assertTrue(result.exception is RuntimeException)
        assertEquals("Unexpected error", result.exception.message)
    }
}
