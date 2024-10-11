package com.jclibs.safetime

import org.apache.commons.net.ntp.TimeInfo

/**
 * Represents the synchronized time information retrieved from an NTP server or cache.
 *
 * This data class holds the time offset, current corrected timestamp, and other information
 * derived from an NTP server response or a cached result.
 *
 * @property timeOffset The time offset (θ) between the client and server.
 * It is the positive or negative difference in absolute time between the two clocks.
 * A positive value indicates that the client time is ahead of the server time, and a negative
 * value indicates that the client time is behind the server time.
 * [Learn more about θ](https://en.wikipedia.org/wiki/Network_Time_Protocol#Clock_synchronization_algorithm)
 *
 * @property timestamp The corrected current timestamp, calculated using the NTP server response
 * or retrieved from a cached response. This timestamp should be considered more accurate than the
 * client's local time.
 *
 * @property responseTimestamp The local client timestamp recorded at the time of receiving the
 * response packet from the NTP server. This is based on the client's [SafeTime.Builder.elapsedTime].
 *
 * @property timeInfo The [TimeInfo] object received directly from the NTP server. This contains
 * additional detailed information about the NTP packet. If the [SafeTimeInfo] is retrieved from
 * the cache, this field will be `null`.
 */
data class SafeTimeInfo(
    val timeOffset: Long,
    val timestamp: Long,
    val responseTimestamp: Long,
    val timeInfo: TimeInfo?
) {
    companion object
}
