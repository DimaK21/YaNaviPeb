package ru.kryu.yanavipeb

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private val scope = MainScope()
    private var demoJob: Job? = null
    private lateinit var binding: ActivityMainBinding
    private val notificationAccessChecker: NotificationAccessChecker by lazy {
        NotificationListenerAccessChecker(applicationContext)
    }
    private val pebbleAppChecker: PebbleAppChecker by lazy {
        PackageManagerPebbleAppChecker(applicationContext)
    }

    private fun refreshStatus() {
        binding.listenerStatus.setText(
            if (notificationAccessChecker.isEnabled()) R.string.status_listener_on else R.string.status_listener_off,
        )
        binding.pebbleStatus.setText(
            if (pebbleAppChecker.isInstalled()) R.string.status_pebble_on else R.string.status_pebble_off,
        )
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
