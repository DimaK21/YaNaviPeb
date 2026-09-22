package ru.kryu.yanavipeb

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.kryu.yanavipeb.databinding.ActivityMainBinding
import ru.kryu.yanavipeb.demo.DemoPlayer
import ru.kryu.yanavipeb.watch.Protocol

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val scope = MainScope()
    private var demoJob: Job? = null

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
        binding.openAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
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

    private fun refreshStatus() {
        val listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        binding.listenerStatus.setText(
            if (listenerEnabled) R.string.status_listener_on else R.string.status_listener_off,
        )
        binding.pebbleStatus.setText(
            if (isPebbleInstalled()) R.string.status_pebble_on else R.string.status_pebble_off,
        )
    }

    @Suppress("DEPRECATION")
    private fun isPebbleInstalled(): Boolean = try {
        packageManager.getPackageInfo(Protocol.PEBBLE_APP_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun toggleDemo() {
        if (demoJob?.isActive == true) {
            demoJob?.cancel()
            return
        }
        binding.demoButton.setText(R.string.demo_stop)
        demoJob = scope.launch {
            try {
                DemoPlayer.play(NavRuntime.syncer(applicationContext))
            } finally {
                binding.demoButton.setText(R.string.demo_start)
            }
        }
    }

}
