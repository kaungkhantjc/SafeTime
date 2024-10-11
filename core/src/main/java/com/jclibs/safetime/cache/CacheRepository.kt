package com.jclibs.safetime.cache

import com.jclibs.safetime.SafeTimeInfo

/**
 * Repository class responsible for managing cached SafeTime information.
 *
 * The [CacheRepository] interacts with a [SafeTimeCache] to store, retrieve, and validate
 * cached SafeTime data. It provides methods for setting new cache entries, checking the validity
 * of the cache, and calculating the current timestamp based on cached data.
 *
 * @param safeTimeCache The [SafeTimeCache] instance used for storing and retrieving cached data.
 */
internal class CacheRepository(private val safeTimeCache: SafeTimeCache) {

    /**
     * Stores the provided [SafeTimeInfo] in the cache.
     *
     * @param safeTimeInfo The [SafeTimeInfo] to be cached.
     */
    fun set(safeTimeInfo: SafeTimeInfo) = safeTimeCache.set(safeTimeInfo)

    /**
     * Checks whether the cached [SafeTimeInfo] is still valid based on the provided elapsed time.
     *
     * The cache is considered invalid if the `responseTimestamp` in the cached [SafeTimeInfo]
     * is greater than the provided [elapsedTime]. If the cache is invalid, it will be cleared.
     *
     * @param elapsedTime The current elapsed time, typically retrieved from a time source like [android.os.SystemClock.elapsedRealtime] in Android.
     * @return `true` if the cache is valid, `false` if the cache is invalid or empty.
     */
    fun hasValidCache(elapsedTime: Long): Boolean {
        val safeTimeInfo = safeTimeCache.get() ?: return false
        val invalidTimestamp = safeTimeInfo.responseTimestamp > elapsedTime
        if (invalidTimestamp) safeTimeCache.clear()
        return !invalidTimestamp
    }

    /**
     * Retrieves the current [SafeTimeInfo] from the cache and adjusts as the correct timestamp based on the
     * provided elapsed time.
     *
     * This method adjusts the `timestamp` field of the cached [SafeTimeInfo] by adding the
     * difference between the provided [elapsedTime] and the cached `responseTimestamp`.
     *
     * @param elapsedTime The current elapsed time, typically retrieved from a time source like [android.os.SystemClock.elapsedRealtime] in Android.
     * @return The adjusted [SafeTimeInfo] with an updated timestamp.
     * @throws NullPointerException if the cache is empty or invalid.
     */
    fun now(elapsedTime: Long): SafeTimeInfo {
        val safeTimeInfo = safeTimeCache.get()!!
        val currentTimestamp = safeTimeInfo.timestamp + (elapsedTime - safeTimeInfo.responseTimestamp)
        return safeTimeInfo.copy(timestamp = currentTimestamp)
    }

}
