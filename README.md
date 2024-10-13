# Safe Time

![Safe Time AI logo](./safe-time.png)

Safe Time is a highly customizable Kotlin library that can be used to get time from [NTP](https://en.wikipedia.org/wiki/Network_Time_Protocol) servers. It should work on JVM-based multiplatform projects including Android.

## Table of Contents

- [Why Safe Time?](#why-safe-time)
- [Download](#download)
- [Usage](#usage)
- [How it works?](#how-it-works)
- [TODO](#todo)
- [Used libraries](#used-libraries)
- [Contributing](#contributing)
- [License](#license)

## Why Safe Time?

If users manually adjust their clocks, `System.currentTimeMillis()` may reflect these changes, leading to unreliable timestamps. Safe Time library ensures a consistent, unaffected time source for your app.

## Download

![Maven Central](https://img.shields.io/maven-central/v/io.github.kaungkhantjc/safeTime-core)

For Android using Gradle

```gradle
implementation("io.github.kaungkhantjc:safeTime-android:1.0.1")
```

For JVM-based multiplatform projects using Gradle

```gradle
implementation("io.github.kaungkhantjc:safeTime-core:1.0.1")
```

[SafeTime jar downloads](https://repo1.maven.org/maven2/io/github/kaungkhantjc/safeTime-core/1.0.1) are available from [Maven Central](https://central.sonatype.com/artifact/io.github.kaungkhantjc/safeTime-core/overview).

## Usage

Create SafeTime instance in application class and sync time once app starts.

```kotlin
class App : Application() {

    val safeTime by lazy {
        SafeTime.Builder()
            .setNTPHosts("time.android.com", "time.google.com")
            .elapsedTimeAPI { SystemClock.elapsedRealtime() }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        safeTime.sync()
    }

}
```

Then we can use like this. We can also use [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) to inject SafeTime instance.

```kotlin
val safeTime = (application as? App)?.safeTime
```

### SafeTime instance functions

- now()

    Retrieves the current timestamp based on cached time. It throws `SafeTimeException` if the cached time is invalid.

    ```kotlin
    val currentTimestamp = safeTime.now()
    ```

- nowOrSync(...)

    Retrieves the current timestamp using cached time or synchronizes with NTP servers.

    ```kotlin
    safeTime.nowOrSync(object: SafeTimeListener {
        override fun onSuccessful(safeTimeInfo: SafeTimeInfo) {
            val currentTimestamp = safeTimeInfo.timestamp
        }
    })
    ```

    We can also use lifecycle-aware coroutine scope for automatic cancellation.

    ```kotlin
    safeTime.nowOrSync(lifecycleScope, safeTimeListener)
    ```

- nowOrElse(...)

    Retrieves the current timestamp from cached time or returns the provided default value

    ```kotlin
    val currentTimestamp = safeTime.nowOrElse { System.currentTimeMillis() }
    ```

- nowOrDefault()

    Retrieves the current timestamp from cached time or the device's `System.currentTimeMillis()` if the cached time is invalid.

    ```kotlin
    val currentTimestamp = safeTime.nowOrDefault()
    ```

- cancel()

    Cancels the synchronization job

    ```kotlin
    safeTime.cancel()
    ```

- sync()

    Synchronizes time with NTP servers. This function checks the validity of the cached time using SafeTimeCache. If the cached time is valid, the synchronization process is skipped.

    ```kotlin
    safeTime.sync()
    ```

- sync(listener)

    Immediately synchronizes time with NTP servers, ignoring cached time.

    ```kotlin
    safeTime.sync(object: SafeTimeListener {
        override fun onSuccessful(safeTimeInfo: SafeTimeInfo) {
            val currentTimestamp = safeTimeInfo.timestamp
        }
    })
    ```

### SafeTime.Builder functions

```kotlin
val safeTime = SafeTime.Builder()
    .setNTPHosts("time.android.com", "time.google.com")
    .addNTPHost("time.aws.com")
    .elapsedTimeAPI { SystemClock.elapsedRealtime() }
    .syncDispatcher(Dispatchers.IO)
    .listenerDispatcher(Dispatchers.Main)
    .setListener(safeTimeListener)
    .cache(PreferenceSafeTimeCache(this))
    .connectionTimeout(3.seconds)
    .maxRetryPerHost(0)
    .maxRetryLoop(0)
    .delayBetweenRetryLoop(0.seconds)
    .rootDelayMax(100)
    .rootDispersionMax(100)
    .serverResponseDelayMax(750.milliseconds)
    .build()
```

## How it works?

Safe Time uses [NTPUDPClient](https://github.com/apache/commons-net/blob/master/src/main/java/org/apache/commons/net/ntp/NTPUDPClient.java) from Apache Commons Net to sync time from NTP servers. More information can be found on the [Apache Commons Net homepage](https://commons.apache.org/proper/commons-net).

For correct time calculation, Safe Time uses [Clock synchronization algorithm](https://en.wikipedia.org/wiki/Network_Time_Protocol#Clock_synchronization_algorithm).

>correct time = response time + clock offset

Response time is calculated like this.

> response time = request time + (response ticks - request ticks)

- request time is obtained from `System.currentTimeMillis()` before NTPUDPClient call is made.
- request ticks is obtained from `System.currentTimeMillis()` (`SystemClock.elapsedRealtime()` in Android) before NTPUDPClient call is made.
- response ticks is obtained from `System.currentTimeMillis()` (`SystemClock.elapsedRealtime()` in Android) after NTPUDPClient call is made.

> The extent of the clock offset θ is directly proportional to the discrepancy between real-time and response time.

To calculates the clock offset θ, we need:

- originate time t0
- receive time t1
- transmit time t2
- response time t3

t0, t1 and t2 are obtained from Apache Commons Net library.

![clock offset θ](https://wikimedia.org/api/rest_v1/media/math/render/svg/9a9ff4428ee0102d2c62b260df1b38b26989b383)

clock offset θ = ((t1 - t0) + (t2 - t3)) / 2

## TODO

- [ ] Full documentation

## Used libraries

- [Apache Commons Net](https://commons.apache.org/proper/commons-net)
- [Kotlin coroutines](https://github.com/Kotlin/kotlinx.coroutines)

> Safe Time is inspired by the awesome [TrueTime for Android](https://github.com/instacart/truetime-android) library. Code structure is inspired by the awesome [Coil](https://github.com/coil-kt/coil) library.

## Contributing

We'd love to accept your patches and contributions to this project. All submissions, including submissions by project members, require review. We use GitHub pull requests for this purpose. Consult GitHub Help for more information on using pull requests.

Please perform a quick search to check if there are already existing issues or pull requests related to your contribution.

## License

Safe Time is released under the Apache 2.0 license.

    Copyright 2024 Safe Text Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
