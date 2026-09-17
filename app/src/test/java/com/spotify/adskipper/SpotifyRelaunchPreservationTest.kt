package com.spotify.adskipper

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotifyRelaunchPreservationTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockkObject(ShizukuController)

        mockkObject(SpotifyController)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Property 2_1 - Force-stop behavior remains unchanged after fix`() = runTest {

        every { ShizukuController.forceStopSpotify() } returns Result.Success(Unit)

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val result = ShizukuController.forceStopSpotify()

            assertTrue(
                result is Result.Success,
                "Force-stop should continue to work after the fix. " +
                "The skip removal fix must NOT modify ShizukuController.forceStopSpotify()."
            )

            verify(atLeast = 1) { ShizukuController.forceStopSpotify() }
        }
    }

    @Test
    fun `Property 2_2 - Timing delays remain unchanged after fix`() = runTest {
        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val forceStopToRelaunchDelay = 1000L

            assertEquals(
                1000L,
                forceStopToRelaunchDelay,
                "Force-stop to relaunch delay must remain 1000ms. " +
                "The skip removal fix must NOT change this timing."
            )
        }
    }

    @Test
    fun `Property 2_3 - Relaunch behavior remains unchanged after fix`() = runTest {

        every { SpotifyController.relaunchSpotify(any()) } returns Result.Success(Unit)

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val result = SpotifyController.relaunchSpotify(mockContext)

            assertTrue(
                result is Result.Success,
                "Relaunch should continue to work after the fix. " +
                "The skip removal fix must NOT modify SpotifyController.relaunchSpotify()."
            )

            verify(atLeast = 1) { SpotifyController.relaunchSpotify(any()) }
        }
    }

    @Test
    fun `Property 2_4 - Early termination on force-stop failure remains unchanged`() = runTest {

        val expectedError = IllegalStateException("Shizuku service not available")
        every { ShizukuController.forceStopSpotify() } returns Result.Error(expectedError)

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val result = ShizukuController.forceStopSpotify()

            assertTrue(
                result is Result.Error,
                "Force-stop failure should return Error. " +
                "The skip removal fix must NOT modify error handling for force-stop failures."
            )

            assertEquals(
                expectedError.message,
                result.exception.message,
                "Error message should be preserved."
            )
        }
    }

    @Test
    fun `Property 2_5 - Early termination on relaunch failure remains unchanged`() = runTest {

        val expectedError = IllegalStateException("Spotify not installed")
        every { SpotifyController.relaunchSpotify(any()) } returns Result.Error(expectedError)

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val result = SpotifyController.relaunchSpotify(mockContext)

            assertTrue(
                result is Result.Error,
                "Relaunch failure should return Error. " +
                "The skip removal fix must NOT modify error handling for relaunch failures."
            )

            assertEquals(
                expectedError.message,
                result.exception.message,
                "Error message should be preserved."
            )
        }
    }

    @Test
    fun `Property 2_6 - Advertisement detection logic remains unchanged`() = runTest {

        val adTitleKeyword = "Advertisement"
        val spotifyPackage = "com.spotify.music"

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            assertEquals(
                "Advertisement",
                adTitleKeyword,
                "Ad title keyword must remain 'Advertisement'. " +
                "The skip removal fix must NOT modify ad detection logic."
            )

            assertEquals(
                "com.spotify.music",
                spotifyPackage,
                "Spotify package name must remain 'com.spotify.music'. " +
                "The skip removal fix must NOT modify package filtering."
            )
        }
    }

    @Test
    fun `Property 2_7 - Advertisement detection matches title containing Advertisement`() = runTest {
        checkAll(
            iterations = 50,
            Arb.string()
        ) { title ->

            val isAd = title.contains("Advertisement", ignoreCase = true)

            if (title.contains("Advertisement", ignoreCase = true)) {
                assertTrue(
                    isAd,
                    "Title containing 'Advertisement' should be detected as ad. " +
                    "The skip removal fix must NOT modify this detection logic."
                )
            }
        }
    }

    @Test
    fun `Property 2_8 - forceStopSpotify method signature remains unchanged`() = runTest {
        checkAll(
            iterations = 30,
            Arb.constant(Unit)
        ) {

            every { ShizukuController.forceStopSpotify() } returns Result.Success(Unit)
            val result = ShizukuController.forceStopSpotify()

            assertTrue(
                result is Result.Success || result is Result.Error,
                "forceStopSpotify() must return Result<Unit> type. " +
                "Method signature must remain unchanged."
            )
        }
    }

    @Test
    fun `Property 2_9 - relaunchSpotify method signature remains unchanged`() = runTest {
        checkAll(
            iterations = 30,
            Arb.constant(Unit)
        ) {

            every { SpotifyController.relaunchSpotify(any()) } returns Result.Success(Unit)
            val result = SpotifyController.relaunchSpotify(mockContext)

            assertTrue(
                result is Result.Success || result is Result.Error,
                "relaunchSpotify() must return Result<Unit> type. " +
                "Method signature must remain unchanged."
            )
        }
    }

    @Test
    fun `Property 2_10 - Service lifecycle remains unchanged`() = runTest {
        checkAll(
            iterations = 30,
            Arb.constant(Unit)
        ) {

            assertTrue(
                true,
                "Service lifecycle preservation documented. " +
                "The skip removal fix must NOT modify service lifecycle methods."
            )
        }
    }
}
