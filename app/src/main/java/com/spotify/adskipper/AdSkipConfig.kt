package com.spotify.adskipper

/**
 * Configuration data class for ad skip sequence timing parameters.
 * 
 * @property forceStopDelayMs Delay in milliseconds between force-stop and relaunch operations.
 *                            Must be between 500ms and 3000ms inclusive.
 * @property skipDelayMs Delay in milliseconds between relaunch and skip operations.
 *                       Must be between 1000ms and 5000ms inclusive.
 * @property spotifyPackageName The package name of the Spotify application.
 * @property adTitleKeyword The notification title keyword that identifies advertisements.
 * 
 * @throws IllegalArgumentException if timing parameters are outside valid ranges.
 */
data class AdSkipConfig(
    val forceStopDelayMs: Long = 1000L,
    val skipDelayMs: Long = 2000L,
    val spotifyPackageName: String = "com.spotify.music",
    val adTitleKeyword: String = "Advertisement"
) {
    init {
        require(forceStopDelayMs in 500..3000) {
            "forceStopDelayMs must be between 500 and 3000 milliseconds, got $forceStopDelayMs"
        }
        require(skipDelayMs in 1000..5000) {
            "skipDelayMs must be between 1000 and 5000 milliseconds, got $skipDelayMs"
        }
        require(spotifyPackageName.isNotBlank()) {
            "spotifyPackageName must not be blank"
        }
        require(adTitleKeyword.isNotBlank()) {
            "adTitleKeyword must not be blank"
        }
    }
}
