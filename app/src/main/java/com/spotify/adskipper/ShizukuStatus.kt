package com.spotify.adskipper

/**
 * Represents the current status of the Shizuku service for UI display.
 * 
 * This enum is used by MainActivity to determine which status message and
 * action button to display to the user during permission setup.
 */
enum class ShizukuStatus {
    /**
     * Shizuku service is running and API_V23 permission has been granted.
     * The app is ready to execute force-stop commands.
     */
    RUNNING_AND_GRANTED,
    
    /**
     * Shizuku service is running but API_V23 permission has not been granted.
     * User needs to grant permission via the permission request dialog.
     */
    RUNNING_NOT_GRANTED,
    
    /**
     * Shizuku service is not currently running.
     * User needs to open the Shizuku app and start the service.
     */
    NOT_RUNNING,
    
    /**
     * Shizuku app is not installed on the device.
     * User needs to install Shizuku from GitHub releases.
     */
    NOT_INSTALLED
}
