package com.dls.jalsanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dls.jalsanit.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var securityEngine: SecurityEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securityEngine = SecurityEngine(this)
        if (securityEngine.isSetupComplete()) {
            startActivity(Intent(this, UnlockActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val anonId = securityEngine.getOrCreateAnonymousId()
        binding.tvGeneratedAnonId.text = anonId

        binding.btnCompleteSetup.setOnClickListener {
            val primaryPin = binding.etPrimaryPin.text?.toString().orEmpty()
            val backupPin = binding.etBackupPin.text?.toString().orEmpty()

            val errorMessage = securityEngine.validateNewPins(primaryPin, backupPin)
            if (errorMessage != null) {
                binding.tvSetupError.text = errorMessage
                binding.tvSetupError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val saved = securityEngine.savePinsAndCompleteSetup(primaryPin, backupPin)
            if (!saved) {
                binding.tvSetupError.text = "Could not save PINs. Please try again."
                binding.tvSetupError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.tvSetupError.visibility = View.GONE
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
