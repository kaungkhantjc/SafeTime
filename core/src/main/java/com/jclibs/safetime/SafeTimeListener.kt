package com.jclibs.safetime

import kotlin.time.Duration

/**
 * Interface for listening to various events related to SafeTime operations.
 *
 * Implement this interface to receive notifications about successful and failed
 * SafeTime sync operations, as well as detailed information about NTP responses and retry logic.
 */
interface SafeTimeListener {

    /**
     * Called when the SafeTime sync operation is successful or valid cached time exists.
     *
     * @param safeTimeInfo Contains the correct time information retrieved during the successful SafeTime operation or from the valid cached time
     */
    fun onSuccessful(safeTimeInfo: SafeTimeInfo) {}

    /**
     * Called when the SafeTime sync operation fails.
     *
     * @param t The exception or error that caused the failure.
     */
    fun onFailed(t: Throwable) {}

    /**
     * Called when an NTP response is successfully received.
     *
     * Provides details about the NTP host, the number of retries for the host, and the current retry loop.
     *
     * @param safeTimeInfo Contains the information retrieved during the successful NTP response.
     * @param host The NTP host that responded.
     * @param retryPerHost The number of retries attempted for the current host.
     * @param retryLoop The current retry loop count across all hosts.
     */
    fun onNTPResponseSuccessful(
        safeTimeInfo: SafeTimeInfo,
        host: String,
        retryPerHost: Int,
        retryLoop: Int
    ) {
    }

    /**
     * Called when an NTP response fails.
     *
     * Provides details about the failed NTP host, the number of retries attempted, the current retry loop,
     * and the exception that caused the failure.
     *
     * @param host The NTP host that failed to respond.
     * @param retryPerHost The number of retries attempted for the current host.
     * @param retryLoop The current retry loop count across all hosts.
     * @param e The exception that caused the failure.
     */
    fun onNTPResponseFailed(host: String, retryPerHost: Int, retryLoop: Int, e: Exception) {}

    /**
     * Called when the next retry loop is scheduled.
     *
     * Provides details about the next retry loop count and the delay before the next retry attempt.
     *
     * @param retryLoop The current retry loop count.
     * @param delay The duration of the delay before the next retry attempt.
     */
    fun nextRetryLoopIn(retryLoop: Int, delay: Duration) {}

}
