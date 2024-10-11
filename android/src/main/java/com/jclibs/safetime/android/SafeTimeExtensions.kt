package com.jclibs.safetime.android

import com.jclibs.safetime.SafeTimeInfo
import org.json.JSONException
import org.json.JSONObject

object SafeTimeExtensions {

    /**
     * Serializes this [SafeTimeInfo] into a JSON string.
     *
     * The resulting JSON will contain the time offset, timestamp, and response timestamp,
     * but will not include the [SafeTimeInfo.timeInfo] field.
     *
     * @return A JSON string representing this [SafeTimeInfo].
     */
    fun SafeTimeInfo.toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("time_offset", timeOffset)
        jsonObject.put("timestamp", timestamp)
        jsonObject.put("response_timestamp", responseTimestamp)
        return jsonObject.toString()
    }

    /**
     * Deserializes a JSON string into a [SafeTimeInfo] object.
     *
     * This function parses the given JSON string to extract the `timeOffset`, `timestamp`,
     * and `responseTimestamp` fields to create a [SafeTimeInfo] object. The [SafeTimeInfo.timeInfo] field
     * is set to `null`, as the JSON does not contain this information.
     *
     * @param json The JSON string representing a serialized [SafeTimeInfo].
     * @return A [SafeTimeInfo] object created from the parsed JSON, or `null` if the
     * JSON is invalid or parsing fails.
     */
    fun SafeTimeInfo.Companion.parse(json: String): SafeTimeInfo? {
        return try {
            val jsonObject = JSONObject(json)
            SafeTimeInfo(
                timeOffset = jsonObject.optLong("time_offset"),
                timestamp = jsonObject.optLong("timestamp"),
                responseTimestamp = jsonObject.optLong("response_timestamp"),
                timeInfo = null
            )
        } catch (ignored: JSONException) {
            null
        }
    }

}