package sample.app

import android.app.Application
import android.os.SystemClock
import com.jclibs.safetime.SafeTime
import kotlinx.coroutines.Dispatchers

class App : Application() {

    val safeTime by lazy {
        SafeTime.Builder()
            .setNTPHosts("time.android.com", "time.google.com")
            .elapsedTimeAPI { SystemClock.elapsedRealtime() }
            .listenerDispatcher(Dispatchers.Main)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        safeTime.sync()
    }

}