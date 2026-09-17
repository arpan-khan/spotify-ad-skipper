package com.spotify.adskipper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class SpotifyRelaunchBugConditionTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        every { mockContext.packageManager } returns mockPackageManager

        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkObject(ShizukuController)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Property 1 - relaunchSpotify from background context successfully opens Spotify`() = runTest {

        every { ShizukuController.launchActivity(any(), any()) } returns Result.Success(Unit)

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val result = SpotifyController.relaunchSpotify(mockContext)

            assertTrue(
                result is Result.Success,
                "Expected Spotify to relaunch successfully from background context via Shizuku. " +
                "On unfixed code, this fails due to Android 15 background launch restrictions. " +
                "On fixed code, this succeeds via Shizuku elevated privileges."
            )

            verify(atLeast = 1) { ShizukuController.launchActivity("com.spotify.music", mockContext) }
        }
    }

    @Test
    fun `relaunchSpotify returns Error when Spotify not installed (preserved behavior)`() {

        every { ShizukuController.launchActivity("com.spotify.music", mockContext) } returns
            Result.Error(IllegalStateException("Package com.spotify.music not installed or has no launch intent"))

        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns null

        val result = SpotifyController.relaunchSpotify(mockContext)

        assertTrue(result is Result.Error, "Expected Error when Spotify not installed, got: $result")
        assertTrue(
            result.exception.message?.contains("not installed") == true ||
            result.exception.message?.contains("no launch intent") == true ||
            result.exception.message?.contains("Spotify not installed") == true,
            "Expected error message about Spotify not being installed, got: ${result.exception.message}"
        )
    }

    @Test
    fun `relaunchSpotify falls back to standard launch when Shizuku unavailable`() {

        every { ShizukuController.launchActivity(any(), any()) } returns
            Result.Error(IllegalStateException("Shizuku service not available"))

        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") } returns mockIntent
        every { mockContext.startActivity(any()) } just Runs

        val result = SpotifyController.relaunchSpotify(mockContext)

        assertTrue(result is Result.Success)

        verify(exactly = 1) { ShizukuController.launchActivity("com.spotify.music", mockContext) }
        verify(exactly = 1) { mockPackageManager.getLaunchIntentForPackage("com.spotify.music") }
        verify(exactly = 1) { mockContext.startActivity(any()) }
    }
}
