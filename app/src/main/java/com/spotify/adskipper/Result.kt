package com.spotify.adskipper

/**
 * Sealed class for type-safe error handling without exceptions.
 * All controller methods return Result<T> instead of throwing exceptions.
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with a value.
     */
    data class Success<T>(val value: T) : Result<T>()
    
    /**
     * Represents a failed operation with an exception.
     */
    data class Error(val exception: Exception) : Result<Nothing>()
}
