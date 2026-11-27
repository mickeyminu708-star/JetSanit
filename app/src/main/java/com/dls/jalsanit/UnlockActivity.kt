package com.dls.jalsanit

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.dls.jalsanit.databinding.ActivityUnlockBinding

class UnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockBinding
    private lateinit var securityEngine: SecurityEngine
    private var timer: CountDownTimer? = null

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
            lockForSeconds(activeCooldown)
        }

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.etUnlockPin.text?.toString().orEmpty()
            when (val result = securityEngine.verifyPin(enteredPin)) {
                is SecurityEngine.UnlockResult.Unlocked -> {
                    timer?.cancel()
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
                    lockForSeconds(result.secondsRemaining)
                }
            }
        }
    }

    private fun lockForSeconds(seconds: Long) {
        timer?.cancel()
        binding.btnUnlock.isEnabled = false
        binding.etUnlockPin.isEnabled = false

        val millis = seconds.coerceAtLeast(1L) * 1000L
        timer = object : CountDownTimer(millis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val left = (millisUntilFinished / 1000L).coerceAtLeast(1L)
                binding.tvUnlockStatus.text = "Locked for ${left}s after 3 wrong attempts."
            }

            override fun onFinish() {
                binding.btnUnlock.isEnabled = true
                binding.etUnlockPin.isEnabled = true
                binding.tvUnlockStatus.text = ""
            }
        }.start()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_USED_BACKUP_PIN = "used_backup_pin"
    }
}
