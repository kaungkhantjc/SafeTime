package com.jclibs.safetime.cache

import com.jclibs.safetime.SafeTimeInfo

/**
 * Interface for caching SafeTime information.
 *
 * This interface defines the basic operations for storing, retrieving, and clearing
 * cached [SafeTimeInfo] data. Implementations of this interface should handle the
 * persistence and management of SafeTime information.
 */
interface SafeTimeCache {

    /**
     * Stores the provided [SafeTimeInfo] in the cache.
     *
     * @param safeTimeInfo The [SafeTimeInfo] instance to be cached.
     */
    fun set(safeTimeInfo: SafeTimeInfo)

    /**
     * Retrieves the cached [SafeTimeInfo].
     *
     * @return The cached [SafeTimeInfo], or `null` if no valid cache exists.
     */
    fun get(): SafeTimeInfo?

    /**
     * Clears the cached [SafeTimeInfo].
     *
     * This method removes any existing cached data, effectively resetting the cache.
     */
    fun clear()
}
