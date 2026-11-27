package com.dls.jalsanit

import android.content.Context
import java.security.MessageDigest
import kotlin.random.Random

class SecurityEngine(context: Context) {

    private val prefs = context.getSharedPreferences("jalsanit_secure_prefs", Context.MODE_PRIVATE)

    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_DONE, false) &&
            !prefs.getString(KEY_ANON_ID, null).isNullOrBlank() &&
            !prefs.getString(KEY_PRIMARY_PIN_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_BACKUP_PIN_HASH, null).isNullOrBlank()
    }

    fun getOrCreateAnonymousId(): String {
        val existing = prefs.getString(KEY_ANON_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val part1 = Random.nextInt(1000, 9999)
        val part2 = Random.nextInt(1000, 9999)
        val generated = "JS-$part1-$part2"
        prefs.edit().putString(KEY_ANON_ID, generated).apply()
        return generated
    }

    fun getAnonymousId(): String {
        return prefs.getString(KEY_ANON_ID, "JS-0000-0000") ?: "JS-0000-0000"
    }

    fun validateNewPins(primaryPin: String, backupPin: String): String? {
        val cleanPrimary = primaryPin.trim()
        val cleanBackup = backupPin.trim()

        val validPattern = Regex("^[A-Za-z0-9]{3,4}$")
        if (!validPattern.matches(cleanPrimary)) {
            return "Primary PIN must be 3-4 letters or numbers."
        }
        if (!validPattern.matches(cleanBackup)) {
            return "Backup PIN must be 3-4 letters or numbers."
        }
        if (cleanPrimary.equals(cleanBackup, ignoreCase = false)) {
            return "Primary PIN and Backup PIN must be different."
        }
        return null
    }

    fun savePinsAndCompleteSetup(primaryPin: String, backupPin: String): Boolean {
        val validationError = validateNewPins(primaryPin, backupPin)
        if (validationError != null) return false

        val anonId = getOrCreateAnonymousId()
        prefs.edit()
            .putString(KEY_ANON_ID, anonId)
            .putString(KEY_PRIMARY_PIN_HASH, hashPin(primaryPin.trim()))
            .putString(KEY_BACKUP_PIN_HASH, hashPin(backupPin.trim()))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .putBoolean(KEY_SETUP_DONE, true)
            .apply()
        return true
    }

    fun remainingCooldownSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        if (until <= nowMs) return 0L
        return ((until - nowMs) + 999L) / 1000L
    }

    sealed class UnlockResult {
        data class Unlocked(val usedBackup: Boolean) : UnlockResult()
        data class Failed(val attemptsLeftBeforeCooldown: Int) : UnlockResult()
        data class CooldownActive(val secondsRemaining: Long) : UnlockResult()
    }

    fun verifyPin(enteredPin: String, nowMs: Long = System.currentTimeMillis()): UnlockResult {
        val cooldownSec = remainingCooldownSeconds(nowMs)
        if (cooldownSec > 0L) {
            return UnlockResult.CooldownActive(cooldownSec)
        }

        val clean = enteredPin.trim()
        val hash = hashPin(clean)
        val primaryHash = prefs.getString(KEY_PRIMARY_PIN_HASH, null)
        val backupHash = prefs.getString(KEY_BACKUP_PIN_HASH, null)

        if (hash == primaryHash) {
            resetFailures()
            return UnlockResult.Unlocked(usedBackup = false)
        }
        if (hash == backupHash) {
            resetFailures()
            return UnlockResult.Unlocked(usedBackup = true)
        }

        val newFailures = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (newFailures >= MAX_FAILED_ATTEMPTS) {
            val cooldownUntil = nowMs + COOLDOWN_DURATION_MS
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_COOLDOWN_UNTIL, cooldownUntil)
                .apply()
            return UnlockResult.CooldownActive(COOLDOWN_DURATION_MS / 1000L)
        } else {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, newFailures).apply()
            return UnlockResult.Failed(MAX_FAILED_ATTEMPTS - newFailures)
        }
    }

    private fun resetFailures() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_ANON_ID = "anon_id"
        private const val KEY_PRIMARY_PIN_HASH = "primary_pin_hash"
        private const val KEY_BACKUP_PIN_HASH = "backup_pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "cooldown_until"
        private const val MAX_FAILED_ATTEMPTS = 3
        private const val COOLDOWN_DURATION_MS = 15_000L
    }
}
