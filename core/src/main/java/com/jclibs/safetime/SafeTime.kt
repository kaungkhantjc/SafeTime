package com.jclibs.safetime

import com.jclibs.safetime.cache.DefaultSafeTimeCache
import com.jclibs.safetime.cache.SafeTimeCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface SafeTime {

    /**
     * Synchronizes time with NTP servers.
     *
     * This function checks the validity of the cached time using [SafeTimeCache].
     * If the cached time is valid, the synchronization process is skipped.
     * For more details on the cache validation, refer to
     * [com.jclibs.safetime.cache.CacheRepository.hasValidCache].
     *
     * If the cached time is invalid, the function proceeds to sync the current
     * time from the NTP servers.
     *
     * @return A [Job] representing the asynchronous synchronization operation.
     *         Returns null if the synchronization process is not initiated.
     */
    fun sync(): Job?

    /**
     * Immediately synchronizes time with NTP servers, ignoring cached time.
     *
     * Sends a synchronization request to NTP servers without checking the
     * cached time from [SafeTimeCache]. The [SafeTimeListener] is notified of
     * the result, whether successful or failed.
     *
     * @param listener An optional [SafeTimeListener] for receiving the timestamp result
     *                 or errors during synchronization.
     * @return A [Job] representing the asynchronous operation.
     */
    fun sync(listener: SafeTimeListener?): Job

    /**
     * Immediately synchronizes time with NTP servers, ignoring cached time.
     *
     * Sends a synchronization request to NTP servers without checking the
     * cached time from [SafeTimeCache]. The [SafeTimeListener] is notified of
     * the result, whether successful or failed.
     *
     * @param scope An optional lifecycle-aware coroutine scope for automatic cancellation when the lifecycle is destroyed.
     * For more details, see [this link](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope).
     *
     * @param listener An optional [SafeTimeListener] for receiving the timestamp result
     *                 or errors during synchronization.
     * @return A [Job] representing the asynchronous operation.
     */
    fun sync(scope: CoroutineScope?, listener: SafeTimeListener?): Job

    /**
     * Retrieves the current timestamp using cached time or synchronizes with NTP servers.
     *
     * If the cached time is valid, the current timestamp is returned immediately via the
     * provided [SafeTimeListener] in its [SafeTimeListener.onSuccessful] callback.
     * Otherwise, it synchronizes with NTP servers to obtain the timestamp.
     *
     * For more details on the cache validation, refer to
     * [com.jclibs.safetime.cache.CacheRepository.hasValidCache].
     *
     * @param listener An optional [SafeTimeListener] for receiving the timestamp result
     *                 or errors during synchronization.
     * @return A [Job] representing the asynchronous operation, or null if synchronization
     *         is not initiated.
     */
    fun nowOrSync(listener: SafeTimeListener?): Job?

    /**
     * Retrieves the current timestamp using cached time or synchronizes with NTP servers.
     *
     * If the cached time is valid, the current timestamp is returned immediately via the
     * provided [SafeTimeListener] in its [SafeTimeListener.onSuccessful] callback.
     * Otherwise, it synchronizes with NTP servers to obtain the timestamp.
     *
     * For more details on the cache validation, refer to
     * [com.jclibs.safetime.cache.CacheRepository.hasValidCache].
     *
     * @param scope An optional lifecycle-aware coroutine scope for automatic cancellation when the lifecycle is destroyed.
     * For more details, see [this link](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope).
     *
     * @param listener An optional [SafeTimeListener] for receiving the timestamp result
     *                 or errors during synchronization.
     * @return A [Job] representing the asynchronous operation, or null if synchronization
     *         is not initiated.
     */
    fun nowOrSync(scope: CoroutineScope?, listener: SafeTimeListener?): Job?

    /**
     * Retrieves the current timestamp based on cached time.
     *
     * @throws SafeTimeException if the cached time is invalid.
     * @return The current timestamp as a [Long].
     */
    @Throws(SafeTimeException::class)
    fun now(): Long

    /**
     * Retrieves the current timestamp from cached time or returns the provided default value
     * if the cached time is invalid.
     * @param defaultValue Fallback default value if cached time is invalid
     */
    fun nowOrElse(defaultValue: () -> Long): Long

    /**
     * Retrieves the current timestamp from cached time or the device's
     * [System.currentTimeMillis] if the cached time is invalid.
     */
    fun nowOrDefault(): Long

    /**
     * Cancels the synchronization job if it's currently running.
     */
    fun cancel()

    /**
     * Retrieves the current time information synchronously from the specified NTP host.
     *
     * @param ntpHost The hostname of the NTP server to query.
     *
     * @throws java.net.UnknownHostException  if the hostname cannot be resolved to an IP address.
     * @throws SecurityException     if a security manager disallows network access.
     * @throws java.io.IOException            if an error occurs while communicating with the NTP server.
     * @throws SafeTimeException     if the received time information is deemed untrusted.
     */
    fun getTime(ntpHost: String): SafeTimeInfo

    /**
     * Builder class to configure and create an instance of [SafeTime].
     *
     * This class allows setting various configurations for the `SafeTime` instance
     * like dispatchers, time listeners, caching strategies, connection timeout, NTP hosts, and retry limits.
     *
     * @constructor Creates a new `Builder` instance.
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    open class Builder {

        /**
         * Dispatcher for performing synchronization tasks.
         * Defaults to [Dispatchers.IO] for background operations.
         */
        protected open var syncDispatcher: CoroutineDispatcher = Dispatchers.IO

        /**
         * Dispatcher for handling listener events.
         * Can be set to null if not required.
         */
        protected open var listenerDispatcher: CoroutineDispatcher? = null

        /**
         * Listener for safe time updates.
         * Optional listener that can be set to receive updates when time is synchronized.
         */
        protected open var safeTimeListener: SafeTimeListener? = null

        /**
         * Cache implementation for storing time values.
         * Defaults to [DefaultSafeTimeCache].
         */
        protected open var safeTimeCache: SafeTimeCache = DefaultSafeTimeCache()

        /**
         * A lambda to provide the elapsed time, by default returns the current system time.
         * In Android, we can use [android.os.SystemClock.elapsedRealtime].
         */
        protected open var elapsedTime: () -> Long = { System.currentTimeMillis() }

        /**
         * The connection timeout for network time requests.
         * Defaults to 3 seconds.
         */
        protected open var connectionTimeout: Duration = 3.seconds

        /**
         * List of NTP hosts used for time synchronization.
         * Can be modified by using [setNTPHosts] or [addNTPHost].
         */
        protected open var ntpHosts: ArrayList<String> = arrayListOf()

        /**
         * Maximum number of retry attempts per host.
         * Defaults to 0.
         */
        protected open var maxRetryPerHost = 0

        /**
         * Maximum number of retry loops for all hosts.
         * Defaults to 0.
         */
        protected open var maxRetryLoop = 0

        /**
         * The delay between retry loops in milliseconds.
         * Defaults to 0, meaning no delay between retry loops.
         */
        protected open var delayBetweenRetryLoop = Duration.ZERO

        /**
         * Maximum allowed root delay as defined in NTP spec RFC-1305
         * Defaults to 100.
         */
        protected open var rootDelayMax = 100

        /**
         * Maximum allowed root dispersion as defined in NTP spec RFC-1305
         * Defaults to 100.
         */
        protected open var rootDispersionMax = 100

        /**
         * Maximum response delay from the server in milliseconds.
         * Defaults to 750 milliseconds.
         */
        protected open var serverResponseDelayMax: Duration = 750.milliseconds

        /**
         * Sets the [CoroutineDispatcher] for synchronization tasks.
         *
         * @param dispatcher The dispatcher to use for synchronization tasks.
         * @return This builder instance for chaining.
         */
        fun syncDispatcher(dispatcher: CoroutineDispatcher) = apply {
            syncDispatcher = dispatcher
        }

        /**
         * Sets the [CoroutineDispatcher] for listener events.
         *
         * @param dispatcher The dispatcher to use for listener events.
         * @return This builder instance for chaining.
         */
        fun listenerDispatcher(dispatcher: CoroutineDispatcher) = apply {
            listenerDispatcher = dispatcher
        }

        /**
         * Sets the [SafeTimeListener] to receive time update events.
         *
         * @param listener The listener to notify on time synchronization events.
         * @return This builder instance for chaining.
         */
        fun setListener(listener: SafeTimeListener) = apply {
            safeTimeListener = listener
        }

        /**
         * Sets the [SafeTimeCache] implementation.
         *
         * @param cache The cache implementation to store time values.
         * @return This builder instance for chaining.
         */
        fun cache(cache: SafeTimeCache) = apply {
            safeTimeCache = cache
        }

        /**
         * Sets a custom elapsed time provider.
         * In Android, we can use [android.os.SystemClock.elapsedRealtime].
         *
         * @param elapsedTimeAPI A lambda that returns the elapsed time.
         * @return This builder instance for chaining.
         */
        fun elapsedTimeAPI(elapsedTimeAPI: () -> Long) = apply {
            elapsedTime = elapsedTimeAPI
        }

        /**
         * Sets the connection timeout for network time synchronization.
         *
         * @param timeout The connection timeout duration.
         * @return This builder instance for chaining.
         */
        fun connectionTimeout(timeout: Duration) = apply {
            this.connectionTimeout = timeout
        }

        /**
         * Sets the list of NTP hosts used for time synchronization.
         *
         * @param host A variable number of NTP host names.
         * @return This builder instance for chaining.
         */
        fun setNTPHosts(vararg host: String) = apply {
            ntpHosts.clear()
            ntpHosts.addAll(host)
        }

        /**
         * Adds a single NTP host to the existing list.
         *
         * @param host The NTP host to add.
         * @return This builder instance for chaining.
         */
        fun addNTPHost(host: String) = apply {
            ntpHosts.add(host)
        }

        /**
         * Sets the maximum number of retries per host.
         *
         * @param value The maximum retry count, must be between 0 and [Int.MAX_VALUE].
         * @throws IllegalArgumentException if the value is out of bounds.
         * @return This builder instance for chaining.
         */
        fun maxRetryPerHost(value: Int) = apply {
            require(value in 0..Int.MAX_VALUE) { "maxRetryPerHost must be between 0 and Int.MAX_VALUE" }
            maxRetryPerHost = value
        }

        /**
         * Sets the maximum retry loop count.
         *
         * @param value The maximum loop count, must be between 0 and [Int.MAX_VALUE].
         * @throws IllegalArgumentException if the value is out of bounds.
         * @return This builder instance for chaining.
         */
        fun maxRetryLoop(value: Int) = apply {
            require(value in 0..Int.MAX_VALUE) { "maxRetryLoop must be between 0 and Int.MAX_VALUE" }
            maxRetryLoop = value
        }

        /**
         * Sets delay between retry loops in milliseconds.
         *
         * @param delay The delay duration.
         * @return This builder instance for chaining.
         */
        fun delayBetweenRetryLoop(delay: Duration) = apply {
            delayBetweenRetryLoop = delay
        }

        /**
         * Sets the maximum root delay allowed.
         *
         * @param value The maximum root delay as defined in NTP spec RFC-1305
         * @throws IllegalArgumentException if the value is out of bounds.
         * @return This builder instance for chaining.
         */
        fun rootDelayMax(value: Int) = apply {
            require(value in 1..Int.MAX_VALUE) { "rootDelayMax must be between 1 and Int.MAX_VALUE" }
            rootDelayMax = value
        }

        /**
         * Sets the maximum root dispersion allowed.
         *
         * @param value The maximum root dispersion as defined in NTP spec RFC-1305
         * @throws IllegalArgumentException if the value is out of bounds.
         * @return This builder instance for chaining.
         */
        fun rootDispersionMax(value: Int) = apply {
            require(value in 1..Int.MAX_VALUE) { "rootDispersionMax must be between 1 and Int.MAX_VALUE" }
            rootDispersionMax = value
        }

        /**
         * Sets the maximum allowed server response delay.
         *
         * @param value The maximum server response delay.
         * @return This builder instance for chaining.
         */
        fun serverResponseDelayMax(value: Duration) = apply {
            serverResponseDelayMax = value
        }

        /**
         * Builds and returns an instance of [SafeTime] with the configured options.
         *
         * @return A new instance of [SafeTime].
         */
        fun build(): SafeTime {
            val options = RealSafeTime.Options(
                syncDispatcher = syncDispatcher,
                listenerDispatcher = listenerDispatcher,
                listener = safeTimeListener,
                cache = safeTimeCache,
                elapsedTime = elapsedTime,
                connectionTimeout = connectionTimeout,
                ntpHosts = ntpHosts,
                maxRetryPerHost = maxRetryPerHost,
                maxRetryLoop = maxRetryLoop,
                delayBetweenRetryLoop = delayBetweenRetryLoop,
                rootDelayMax = rootDelayMax,
                rootDispersionMax = rootDispersionMax,
                serverResponseDelayMax = serverResponseDelayMax
            )
            return RealSafeTime(options)
        }
    }


}