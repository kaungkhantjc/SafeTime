package com.jclibs.safetime.cache

import com.jclibs.safetime.SafeTimeInfo

/**
 * Default implementation of the [SafeTimeCache] interface.
 *
 * This class provides a simple in-memory caching mechanism for storing and retrieving
 * [SafeTimeInfo] data. It maintains a single instance of `SafeTimeInfo` and offers methods
 * to set, get, and clear the cached data.
 */
class DefaultSafeTimeCache : SafeTimeCache {

    private var safeTimeInfo: SafeTimeInfo? = null

    /**
     * Stores the provided [SafeTimeInfo] in the cache.
     *
     * @param safeTimeInfo The [SafeTimeInfo] instance to be cached.
     */
    override fun set(safeTimeInfo: SafeTimeInfo) {
        this.safeTimeInfo = safeTimeInfo
    }

    /**
     * Retrieves the cached [SafeTimeInfo].
     *
     * @return The cached [SafeTimeInfo], or `null` if no valid cache exists.
     */
    override fun get(): SafeTimeInfo? = this.safeTimeInfo

    /**
     * Clears the cached [SafeTimeInfo].
     *
     * This method removes the existing cached data, setting it to `null`.
     */
    override fun clear() {
        this.safeTimeInfo = null
    }

}

