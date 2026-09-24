package ru.kryu.yanavipeb

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.kryu.yanavipeb.databinding.ActivityMainBinding
import ru.kryu.yanavipeb.demo.DemoPlayer
import ru.kryu.yanavipeb.demo.DemoScript
import ru.kryu.yanavipeb.nav.NotificationAccessChecker
import ru.kryu.yanavipeb.nav.NotificationListenerAccessChecker
import ru.kryu.yanavipeb.watch.PackageManagerPebbleAppChecker
import ru.kryu.yanavipeb.watch.PebbleAppChecker

class MainActivity : AppCompatActivity() {
    private val scope = MainScope()
    private var demoJob: Job? = null
    private lateinit var binding: ActivityMainBinding
    private val notificationAccessChecker: NotificationAccessChecker by lazy {
        NotificationListenerAccessChecker(applicationContext)
    }
    private val pebbleAppChecker: PebbleAppChecker by lazy {
        PackageManagerPebbleAppChecker(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.openAccessButton.setOnClickListener { showAccessDisclosure() }
        binding.demoButton.setOnClickListener { toggleDemo() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Google Play prominent disclosure: shown before the system settings, and only an explicit
    // tap on the positive button proceeds; dismissing the dialog is not consent.
    private fun showAccessDisclosure() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.access_disclosure_title)
            .setMessage(R.string.access_disclosure_message)
            .setNegativeButton(R.string.access_disclosure_decline, null)
            .setPositiveButton(R.string.access_disclosure_accept) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .show()
    }

    private fun refreshStatus() {
        updateStatusRow(
            binding.listenerStatus,
            notificationAccessChecker.isEnabled(),
            R.string.status_listener_on,
            R.string.status_listener_off,
        )
        updateStatusRow(
            binding.pebbleStatus,
            pebbleAppChecker.isInstalled(),
            R.string.status_pebble_on,
            R.string.status_pebble_off,
        )
    }

    private fun updateStatusRow(view: TextView, ok: Boolean, onText: Int, offText: Int) {
        view.setText(if (ok) onText else offText)
        val icon = if (ok) R.drawable.ic_status_ok else R.drawable.ic_status_warning
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
    }

    private fun toggleDemo() {
        if (demoJob?.isActive == true) {
            demoJob?.cancel()
            return
        }
        binding.demoButton.setText(R.string.demo_stop)
        demoJob = scope.launch {
            try {
                DemoPlayer.play(NavRuntime.syncer(applicationContext), DemoScript.steps(applicationContext))
            } finally {
                binding.demoButton.setText(R.string.demo_start)
            }
        }
    }
}
