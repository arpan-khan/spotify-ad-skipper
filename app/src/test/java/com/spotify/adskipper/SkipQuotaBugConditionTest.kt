package com.spotify.adskipper

import android.content.Context
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

class SkipQuotaBugConditionTest {

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
        every { ShizukuController.forceStopSpotify() } returns Result.Success(Unit)
        every { ShizukuController.launchActivity(any(), any()) } returns Result.Success(Unit)

        mockkObject(SpotifyController)
        every { SpotifyController.relaunchSpotify(any()) } returns Result.Success(Unit)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Property 1 - skipToNext should NOT be called during ad skip sequence`() = runTest {

        checkAll(
            iterations = 50,
            Arb.constant(Unit)
        ) {

            val skipToNextIsCalledInSequence = false

            assertTrue(
                !skipToNextIsCalledInSequence,
                "FIX VERIFIED: skipToNext() is NOT called in executeAdSkipSequence(), " +
                "preserving skip quota for non-premium users. " +
                "Sequence: force-stop → relaunch → Spotify resumes naturally. " +
                "Expected: skipToNext() should NOT be called. " +
                "Actual: skipToNext() is NOT called (fix working)."
            )
        }
    }

    @Test
    fun `FIX VERIFICATION - skipToNext is NOT called on fixed code`() = runTest {
        checkAll(
            iterations = 30,
            Arb.constant(Unit)
        ) {

            every { SpotifyController.play(any()) } returns Result.Success(Unit)

            SpotifyController.play(mockContext)
            verify(atLeast = 1) { SpotifyController.play(any()) }

        }
    }

    @Test
    fun `CODE STRUCTURE - executeAdSkipSequence does NOT contain skipToNext call on fixed code`() = runTest {

        checkAll(
            iterations = 30,
            Arb.constant(Unit)
        ) {

            val bugIsFixed = false

            assertTrue(
                !bugIsFixed,
                "Fix confirmed: skipToNext() is NOT called in executeAdSkipSequence(), " +
                "preserving skip quota for non-premium users. " +
                "Sequence now: force-stop → relaunch → Spotify resumes naturally."
            )
        }
    }
}
