package sample.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.jclibs.safetime.SafeTimeInfo
import com.jclibs.safetime.SafeTimeListener
import kotlinx.coroutines.Job
import sample.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

class MainActivity : Activity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val dateFormat = "EEE, MMM dd yyyy HH:mm:ss.SSS"

    private fun formatDate(timestamp: Long) =
        SimpleDateFormat(dateFormat, Locale.US).format(Date(timestamp))

    private var safeTimeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val safeTime = (application as App).safeTime

        // get current time from cache or sync time from NTP servers if it is invalid
        binding.btnNowOrSync.setOnClickListener {
            safeTimeJob = safeTime.nowOrSync(object : SafeTimeListener {
                override fun onSuccessful(safeTimeInfo: SafeTimeInfo) {
                    val correctTimestamp = safeTimeInfo.timestamp
                    val readableDate = formatDate(correctTimestamp)
                    updateLogs("Now or sync => successful: $readableDate")
                }

                override fun onFailed(t: Throwable) {
                    updateLogs("Now or sync => failed: $t")
                }
            })
        }

        // sync time from NTP servers immediately
        binding.btnSync.setOnClickListener {
            safeTimeJob = safeTime.sync(safeTimeListener)
        }

    }

    private val safeTimeListener = object : SafeTimeListener {
        override fun onSuccessful(safeTimeInfo: SafeTimeInfo) {
            updateLogs("Sync => successful: ${formatDate(safeTimeInfo.timestamp)}")
        }

        override fun onFailed(t: Throwable) {
            updateLogs("Sync => failed: $t")
        }

        override fun onNTPResponseSuccessful(
            safeTimeInfo: SafeTimeInfo,
            host: String,
            retryPerHost: Int,
            retryLoop: Int
        ) {
            updateLogs(
                "Sync => NTPResponse successful [host: $host, retryPerHost: $retryPerHost, retryLoop: $retryLoop]" +
                        "\n${formatDate(safeTimeInfo.timestamp)}"
            )
        }

        override fun onNTPResponseFailed(
            host: String,
            retryPerHost: Int,
            retryLoop: Int,
            e: Exception
        ) {
            updateLogs(
                "Sync => NTPResponse failed [host: $host, retryPerHost: $retryPerHost, retryLoop: $retryLoop]" +
                        "\n$e"
            )
        }

        override fun nextRetryLoopIn(retryLoop: Int, delay: Duration) {
            updateLogs("Sync => next retry $retryLoop will happen in ${delay.inWholeMilliseconds} milliseconds.")
        }

    }

    private fun updateLogs(message: String) {
        binding.tvLog.append("$message\n\n")
        binding.scroll.post {
            binding.scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        safeTimeJob?.cancel()
        super.onDestroy()
    }

}

