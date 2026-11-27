package com.dls.jalsanit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.dls.jalsanit.databinding.ActivityUnlockBinding

class UnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockBinding
    private lateinit var securityEngine: SecurityEngine
    private val timerHandler = Handler(Looper.getMainLooper())
    private var cooldownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securityEngine = SecurityEngine(this)
        if (!securityEngine.isSetupComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvUnlockAnonId.text = "ID: ${securityEngine.getAnonymousId()}"

        val activeCooldown = securityEngine.remainingCooldownSeconds()
        if (activeCooldown > 0L) {
            startCooldownCountdown(activeCooldown)
        }

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.etUnlockPin.text?.toString().orEmpty()
            when (val result = securityEngine.verifyPin(enteredPin)) {
                is SecurityEngine.UnlockResult.Unlocked -> {
                    stopCooldownCountdown()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra(EXTRA_USED_BACKUP_PIN, result.usedBackup)
                    }
                    startActivity(intent)
                    finish()
                }
                is SecurityEngine.UnlockResult.Failed -> {
                    binding.tvUnlockStatus.text =
                        "Wrong PIN. ${result.attemptsLeftBeforeCooldown} attempt(s) left before 15s lock."
                }
                is SecurityEngine.UnlockResult.CooldownActive -> {
                    startCooldownCountdown(result.secondsRemaining)
                }
            }
        }
    }

    private fun startCooldownCountdown(seconds: Long) {
        stopCooldownCountdown()
        binding.btnUnlock.isEnabled = false
        binding.etUnlockPin.isEnabled = false

        var remaining = seconds.coerceAtLeast(1L)
        val tickTask = object : Runnable {
            override fun run() {
                if (remaining > 0L) {
                    binding.tvUnlockStatus.text = "Locked for ${remaining}s after 3 wrong attempts."
                    remaining -= 1L
                    timerHandler.postDelayed(this, 1000L)
                } else {
                    binding.btnUnlock.isEnabled = true
                    binding.etUnlockPin.isEnabled = true
                    binding.tvUnlockStatus.text = ""
                }
            }
        }
        cooldownRunnable = tickTask
        timerHandler.post(tickTask)
    }

    private fun stopCooldownCountdown() {
        cooldownRunnable?.let { timerHandler.removeCallbacks(it) }
        cooldownRunnable = null
    }

    override fun onDestroy() {
        stopCooldownCountdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_USED_BACKUP_PIN = "used_backup_pin"
    }
}
