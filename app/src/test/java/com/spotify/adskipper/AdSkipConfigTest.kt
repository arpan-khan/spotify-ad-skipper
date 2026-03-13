package com.spotify.adskipper

import org.junit.Test
import org.junit.Assert.*

class AdSkipConfigTest {

    @Test
    fun `default configuration has valid timing parameters`() {
        val config = AdSkipConfig()
        
        assertEquals(1000L, config.forceStopDelayMs)
        assertEquals(2000L, config.skipDelayMs)
        assertEquals("com.spotify.music", config.spotifyPackageName)
        assertEquals("Advertisement", config.adTitleKeyword)
    }

    @Test
    fun `forceStopDelayMs accepts minimum valid value`() {
        val config = AdSkipConfig(forceStopDelayMs = 500L)
        assertEquals(500L, config.forceStopDelayMs)
    }

    @Test
    fun `forceStopDelayMs accepts maximum valid value`() {
        val config = AdSkipConfig(forceStopDelayMs = 3000L)
        assertEquals(3000L, config.forceStopDelayMs)
    }

    @Test
    fun `forceStopDelayMs accepts mid-range value`() {
        val config = AdSkipConfig(forceStopDelayMs = 1500L)
        assertEquals(1500L, config.forceStopDelayMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `forceStopDelayMs rejects value below minimum`() {
        AdSkipConfig(forceStopDelayMs = 499L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `forceStopDelayMs rejects value above maximum`() {
        AdSkipConfig(forceStopDelayMs = 3001L)
    }

    @Test
    fun `skipDelayMs accepts minimum valid value`() {
        val config = AdSkipConfig(skipDelayMs = 1000L)
        assertEquals(1000L, config.skipDelayMs)
    }

    @Test
    fun `skipDelayMs accepts maximum valid value`() {
        val config = AdSkipConfig(skipDelayMs = 5000L)
        assertEquals(5000L, config.skipDelayMs)
    }

    @Test
    fun `skipDelayMs accepts mid-range value`() {
        val config = AdSkipConfig(skipDelayMs = 3000L)
        assertEquals(3000L, config.skipDelayMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `skipDelayMs rejects value below minimum`() {
        AdSkipConfig(skipDelayMs = 999L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `skipDelayMs rejects value above maximum`() {
        AdSkipConfig(skipDelayMs = 5001L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `spotifyPackageName rejects blank string`() {
        AdSkipConfig(spotifyPackageName = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `spotifyPackageName rejects whitespace-only string`() {
        AdSkipConfig(spotifyPackageName = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `adTitleKeyword rejects blank string`() {
        AdSkipConfig(adTitleKeyword = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `adTitleKeyword rejects whitespace-only string`() {
        AdSkipConfig(adTitleKeyword = "   ")
    }

    @Test
    fun `custom configuration with all valid parameters`() {
        val config = AdSkipConfig(
            forceStopDelayMs = 1500L,
            skipDelayMs = 2500L,
            spotifyPackageName = "com.custom.package",
            adTitleKeyword = "CustomAd"
        )
        
        assertEquals(1500L, config.forceStopDelayMs)
        assertEquals(2500L, config.skipDelayMs)
        assertEquals("com.custom.package", config.spotifyPackageName)
        assertEquals("CustomAd", config.adTitleKeyword)
    }
}
