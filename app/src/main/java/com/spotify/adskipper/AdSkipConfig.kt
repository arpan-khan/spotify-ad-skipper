package com.spotify.adskipper

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
