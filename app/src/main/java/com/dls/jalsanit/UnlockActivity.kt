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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var remainingSeconds: Long = 0L

    private val countdownAction = Runnable {
        tickCooldown()
    }

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
            beginCooldown(activeCooldown)
        }

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.etUnlockPin.text?.toString().orEmpty()
            when (val result = securityEngine.verifyPin(enteredPin)) {
                is SecurityEngine.UnlockResult.Unlocked -> {
                    stopCooldown()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra(EXTRA_USED_BACKUP_PIN, result.usedBackup)
                    startActivity(intent)
                    finish()
                }
                is SecurityEngine.UnlockResult.Failed -> {
                    binding.tvUnlockStatus.text =
                        "Wrong PIN. ${result.attemptsLeftBeforeCooldown} attempt(s) left before 15s lock."
                }
                is SecurityEngine.UnlockResult.CooldownActive -> {
                    beginCooldown(result.secondsRemaining)
                }
            }
        }
    }

    private fun beginCooldown(seconds: Long) {
        stopCooldown()
        remainingSeconds = seconds.coerceAtLeast(1L)
        binding.btnUnlock.isEnabled = false
        binding.etUnlockPin.isEnabled = false
        mainHandler.post(countdownAction)
    }

    private fun tickCooldown() {
        if (remainingSeconds > 0L) {
            binding.tvUnlockStatus.text = "Locked for ${remainingSeconds}s after 3 wrong attempts."
            remainingSeconds -= 1L
            mainHandler.postDelayed(countdownAction, 1000L)
        } else {
            binding.btnUnlock.isEnabled = true
            binding.etUnlockPin.isEnabled = true
            binding.tvUnlockStatus.text = ""
        }
    }

    private fun stopCooldown() {
        mainHandler.removeCallbacks(countdownAction)
        remainingSeconds = 0L
    }

    override fun onDestroy() {
        stopCooldown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_USED_BACKUP_PIN = "used_backup_pin"
    }
}
