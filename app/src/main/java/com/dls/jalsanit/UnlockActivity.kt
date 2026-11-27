package com.dls.jalsanit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dls.jalsanit.databinding.ActivityUnlockBinding

class UnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockBinding
    private lateinit var securityEngine: SecurityEngine

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
            binding.tvUnlockStatus.text = "Locked for ${activeCooldown}s after 3 wrong attempts."
        }

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.etUnlockPin.text?.toString().orEmpty()
            when (val result = securityEngine.verifyPin(enteredPin)) {
                is SecurityEngine.UnlockResult.Unlocked -> {
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
                    binding.tvUnlockStatus.text =
                        "Locked for ${result.secondsRemaining}s after 3 wrong attempts."
                }
            }
        }
    }

    companion object {
        const val EXTRA_USED_BACKUP_PIN = "used_backup_pin"
    }
}
