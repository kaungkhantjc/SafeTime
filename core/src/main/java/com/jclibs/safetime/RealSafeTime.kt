package com.jclibs.safetime

import com.jclibs.safetime.cache.CacheRepository
import com.jclibs.safetime.cache.SafeTimeCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.Duration

internal class RealSafeTime(
    private val options: Options
) : SafeTime {

    private val cacheRepository = CacheRepository(options.cache)
    private var syncJob: Job? = null

    override fun sync(): Job? = nowOrSync(null, null)

    override fun sync(listener: SafeTimeListener?): Job = syncInternal(null, listener)

    override fun sync(scope: CoroutineScope?, listener: SafeTimeListener?): Job =
        syncInternal(scope, listener)

    override fun nowOrSync(listener: SafeTimeListener?): Job? = nowOrSync(null, listener)

    override fun nowOrSync(scope: CoroutineScope?, listener: SafeTimeListener?): Job? {
        val safeTimeListener = listener ?: options.listener
        val safeTimeInfo = getCachedSafeTimeInfo()
        if (safeTimeInfo != null) {
            safeTimeListener?.onSuccessful(safeTimeInfo)
            return null
        } else {
            return syncInternal(scope, safeTimeListener)
        }
    }

    override fun now(): Long {
        return getCachedSafeTimeInfo()?.timestamp
            ?: throw SafeTimeException("Does not have valid cached time yet.")
    }

    override fun nowOrElse(defaultValue: () -> Long): Long {
        return getCachedSafeTimeInfo()?.timestamp ?: defaultValue()
    }

    override fun nowOrDefault(): Long {
        return nowOrElse { System.currentTimeMillis() }
    }

    override fun cancel() {
        syncJob?.cancel()
    }

    private fun getCachedSafeTimeInfo(): SafeTimeInfo? {
        val elapsedTime = options.elapsedTime()
        val hasValidCache = cacheRepository.hasValidCache(elapsedTime)
        return if (hasValidCache) cacheRepository.now(elapsedTime) else null
    }

    private fun syncInternal(scope: CoroutineScope?, listener: SafeTimeListener?): Job {
        require(options.ntpHosts.isNotEmpty()) { "NTP Hosts should not be empty." }
        val safeTimeListener = listener ?: options.listener

        val coroutineScope =
            scope ?: CoroutineScope(SupervisorJob() + options.syncDispatcher)
        val syncDispatcher = CoroutineName("SafeTime") + options.syncDispatcher
        val listenerDispatcher = options.listenerDispatcher ?: coroutineScope.coroutineContext

        val previousJob = syncJob
        syncJob = coroutineScope.launch(syncDispatcher) {
            previousJob?.cancelAndJoin()

            val totalHosts = options.ntpHosts.size
            var currentHostIndex = 0
            var retriesForCurrentHost = 0
            var retryCycles = 0
            var gotSafeTimeInfo = false
            val delayBetweenRetryLoop = options.delayBetweenRetryLoop.inWholeMilliseconds

            fun shouldSwitchHost(): Boolean = retriesForCurrentHost == options.maxRetryPerHost
            fun isLastHost(): Boolean = currentHostIndex == totalHosts - 1
            fun shouldStopRetrying(): Boolean = retryCycles == options.maxRetryLoop

            while (isActive) {
                val host = options.ntpHosts[currentHostIndex]
                var safeTimeInfo: SafeTimeInfo? = null

                try {
                    safeTimeInfo = getTime(host)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    withContext(listenerDispatcher) {
                        safeTimeListener?.onNTPResponseFailed(host, retriesForCurrentHost, retryCycles, e)
                    }
                }

                if (safeTimeInfo != null) {
                    cacheRepository.set(safeTimeInfo)

                    withContext(listenerDispatcher) {
                        safeTimeListener?.onNTPResponseSuccessful(
                            safeTimeInfo,
                            host,
                            retriesForCurrentHost,
                            retryCycles
                        )
                        safeTimeListener?.onSuccessful(safeTimeInfo)
                    }

                    gotSafeTimeInfo = true
                    break
                }

                // Retry logic
                if (shouldSwitchHost()) {
                    if (isLastHost()) {
                        if (shouldStopRetrying()) {
                            break // Stop retrying after max cycles through all hosts
                        }
                        else {
                            retryCycles++
                            currentHostIndex = 0 // Reset to the first host

                            if (delayBetweenRetryLoop > 0) {
                                withContext(listenerDispatcher) {
                                    safeTimeListener?.nextRetryLoopIn(retryCycles, options.delayBetweenRetryLoop)
                                }
                                delay(delayBetweenRetryLoop)
                            }
                        }
                    } else {
                        currentHostIndex++ // Move to the next host
                    }
                    retriesForCurrentHost = 0 // Reset retry counter for the new host
                } else {
                    retriesForCurrentHost++ // Increment retry for the current host
                }

            }

            ensureActive() // to avoid onFailed() call when cancel
            if (!gotSafeTimeInfo) {
                withContext(listenerDispatcher) {
                    safeTimeListener?.onFailed(SafeTimeException("Failed to sync time."))
                }
            }

        }

        return syncJob!!
    }

    override fun getTime(ntpHost: String): SafeTimeInfo {
        val address = InetAddress.getByName(ntpHost)
        val client = NTPUDPClient()

        // For compatibility with Android versions below 8.0
        @Suppress("DEPRECATION")
        client.defaultTimeout = options.connectionTimeout.inWholeMilliseconds.toInt()

        val requestTime = System.currentTimeMillis()
        val requestTicks = options.elapsedTime()
        val timeInfo = client.use { it.getTime(address) }
        val responseTicks = options.elapsedTime()

        val message = timeInfo.message

        val rootDelay = message.rootDelay
        if (rootDelay > options.rootDelayMax) {
            throw SafeTimeException(
                "Invalid response from NTP server. %s violation. %d [actual] > %d [expected]",
                "root_delay",
                rootDelay,
                options.rootDelayMax
            )
        }

        val rootDispersion = message.rootDispersion
        if (rootDispersion > options.rootDispersionMax) {
            throw SafeTimeException(
                "Invalid response from NTP server. %s violation. %f [actual] > %f [expected]",
                "root_dispersion",
                rootDispersion,
                options.rootDispersionMax
            )
        }

        val mode = message.mode
        if (mode !in setOf(4, 5)) {
            throw SafeTimeException("Untrusted mode value: $mode")
        }

        val stratum = message.stratum
        if (stratum !in 1..15) {
            throw SafeTimeException("Untrusted stratum value: $stratum")
        }

        val leapIndicator = message.leapIndicator
        if (leapIndicator == 3) {
            throw SafeTimeException("Unsynchronized server response received. [leapIndicator: 3]")
        }

        val originateTime = message.originateTimeStamp.time // t0
        val receiveTime = message.receiveTimeStamp.time // t1
        val transmitTime = message.transmitTimeStamp.time // t2
        val responseTime = requestTime + (responseTicks - requestTicks) // t3

        val delay = abs((responseTime - originateTime) - (transmitTime - receiveTime))
        val serverResponseDelayMaxValue = options.serverResponseDelayMax.inWholeMilliseconds

        if (delay >= serverResponseDelayMaxValue) {
            throw SafeTimeException(
                "%s too large for comfort %d [actual] >= %f [expected]",
                "server_response_delay",
                delay.toInt(),
                serverResponseDelayMaxValue.toInt()
            )
        }

        val timeElapsedSinceRequest = abs((originateTime - System.currentTimeMillis()))
        if (timeElapsedSinceRequest >= 10000) {
            throw SafeTimeException(
                "Request was sent more than 10 seconds back " +
                        timeElapsedSinceRequest
            )
        }

        val clockOffset =
            calculateClockOffset(originateTime, receiveTime, transmitTime, responseTime)
        val correctTimestamp = responseTime + clockOffset

        return SafeTimeInfo(
            timeOffset = clockOffset,
            timestamp = correctTimestamp,
            responseTimestamp = responseTicks,
            timeInfo = timeInfo
        )
    }

    /**
     * Calculate tme offset θ.
     * It is positive or negative (client time > server time) difference in absolute time between the two clocks.
     * [Learn more about θ](https://en.wikipedia.org/wiki/Network_Time_Protocol#Clock_synchronization_algorithm)
     */
    private fun calculateClockOffset(t0: Long, t1: Long, t2: Long, t3: Long) =
        ((t1 - t0) + (t2 - t3)) / 2


    data class Options(
        val syncDispatcher: CoroutineDispatcher,
        val listenerDispatcher: CoroutineDispatcher?,
        val listener: SafeTimeListener?,
        val cache: SafeTimeCache,
        val elapsedTime: () -> Long,
        val connectionTimeout: Duration,
        val ntpHosts: ArrayList<String>,
        val maxRetryPerHost: Int,
        val maxRetryLoop: Int,
        val delayBetweenRetryLoop: Duration,
        val rootDelayMax: Int,
        val rootDispersionMax: Int,
        val serverResponseDelayMax: Duration,
    )

}