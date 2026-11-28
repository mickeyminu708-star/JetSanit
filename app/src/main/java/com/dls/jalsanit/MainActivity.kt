package com.dls.jalsanit

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.dls.jalsanit.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var securityEngine: SecurityEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securityEngine = SecurityEngine(this)
        if (!securityEngine.isSetupComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usedBackupPin = intent?.getBooleanExtra(UnlockActivity.EXTRA_USED_BACKUP_PIN, false) ?: false
        if (usedBackupPin) {
            Toast.makeText(
                this,
                "Important: Unlocked using Backup PIN (${securityEngine.getAnonymousId()}).",
                Toast.LENGTH_LONG
            ).show()
        }

        setupToolbarAndDrawer()
        setupNavigationListeners()
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.subtitle = "ID: ${securityEngine.getAnonymousId()} • Offline Shield"
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigationListeners() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_shield -> {
                    showSectionNotice("Shield Active", "Offline protection & scam rules are active locally.")
                    true
                }
                R.id.nav_scanner -> {
                    showSectionNotice("Link & Media Scanner", "Paste or share suspicious links/messages for local inspection.")
                    true
                }
                R.id.nav_logs -> {
                    showSectionNotice("Security Logs", "Local scan & unlock history stays on this device.")
                    true
                }
                R.id.nav_vault -> {
                    showSectionNotice("Important Rules & Vault", getImportantOfflineRules())
                    true
                }
                else -> false
            }
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(Gravity.START)
            when (menuItem.itemId) {
                R.id.nav_vault, R.id.nav_shield -> {
                    showSectionNotice("Important Offline Rules", getImportantOfflineRules())
                    true
                }
                R.id.nav_scanner -> {
                    showSectionNotice("Scanner", "Ready to inspect shared text or URLs locally.")
                    true
                }
                R.id.nav_logs -> {
                    showSectionNotice("Logs", "No external cloud server is connected.")
                    true
                }
                else -> false
            }
        }
    }

    private fun getImportantOfflineRules(): String {
        return "Important:\n" +
            "1. No phone number or cloud account required.\n" +
            "2. Primary PIN and Backup PIN must be different (3-4 chars/digits).\n" +
            "3. 3 wrong PIN entries trigger a 15-second cooldown lock.\n" +
            "4. All data stays locally on your device."
    }

    private fun showSectionNotice(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
