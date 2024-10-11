package com.jclibs.safetime.android

import android.content.Context
import android.content.SharedPreferences
import com.jclibs.safetime.SafeTimeInfo
import com.jclibs.safetime.android.SafeTimeExtensions.parse
import com.jclibs.safetime.android.SafeTimeExtensions.toJson
import com.jclibs.safetime.cache.SafeTimeCache

/**
 * A [SafeTimeCache] implementation that uses Android's [SharedPreferences] to store
 * and retrieve [SafeTimeInfo] objects.
 *
 * This class provides mechanisms to persist safe time information across application
 * sessions using shared preferences. It supports multiple constructors for flexibility
 * in initialization.
 *
 * @constructor Creates a [PreferenceSafeTimeCache] using the provided [Context].
 * It initializes the shared preferences with a default key based on the application's package name.
 *
 * @constructor Creates a [PreferenceSafeTimeCache] using the provided [SharedPreferences].
 * It uses the default cache key.
 *
 * @constructor Creates a [PreferenceSafeTimeCache] using the provided [SharedPreferences] and a custom cache key.
 *
 * @see SafeTimeCache
 */
@Suppress("unused")
class PreferenceSafeTimeCache : SafeTimeCache {

    companion object {
        /**
         * The default key used to store the safe time cache in [SharedPreferences].
         */
        const val KEY_CACHE = "safe_time_cache"
    }

    private val sharedPreferencesKey: String
    private val sharedPreferences: SharedPreferences

    /**
     * Constructs a [PreferenceSafeTimeCache] with the given [Context].
     *
     * Initializes [SharedPreferences] using the application's package name with a "_preferences" suffix.
     *
     * @param context The context used to access the shared preferences.
     */
    constructor(context: Context) {
        this.sharedPreferences =
            context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        this.sharedPreferencesKey = KEY_CACHE
    }

    /**
     * Constructs a [PreferenceSafeTimeCache] with the provided [SharedPreferences].
     *
     * Uses the default cache key [KEY_CACHE].
     *
     * @param sharedPreferences The shared preferences instance to use for caching.
     */
    constructor(sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
        this.sharedPreferencesKey = KEY_CACHE
    }

    /**
     * Constructs a [PreferenceSafeTimeCache] with the provided [SharedPreferences] and custom key.
     *
     * @param sharedPreferences The shared preferences instance to use for caching.
     * @param key The custom key under which the safe time information will be stored.
     */
    constructor(sharedPreferences: SharedPreferences, key: String) {
        this.sharedPreferences = sharedPreferences
        this.sharedPreferencesKey = key
    }

    /**
     * Stores the given [SafeTimeInfo] in the shared preferences.
     *
     * The [SafeTimeInfo] is serialized to a JSON string before being saved.
     *
     * @param safeTimeInfo The safe time information to store.
     */
    override fun set(safeTimeInfo: SafeTimeInfo) {
        sharedPreferences.edit().putString(sharedPreferencesKey, safeTimeInfo.toJson()).apply()
    }

    /**
     * Retrieves the stored [SafeTimeInfo] from the shared preferences.
     *
     * Parses the JSON string back into a [SafeTimeInfo] object. If parsing fails or
     * no data is found, returns `null`.
     *
     * @return The retrieved [SafeTimeInfo] or `null` if not available or parsing fails.
     */
    override fun get(): SafeTimeInfo? {
        val json = sharedPreferences.getString(sharedPreferencesKey, null) ?: return null
        return SafeTimeInfo.parse(json)
    }

    /**
     * Clears the stored [SafeTimeInfo] from the shared preferences.
     *
     * Removes the entry associated with the cache key.
     */
    override fun clear() {
        sharedPreferences.edit().remove(sharedPreferencesKey).apply()
    }

}
