package com.hermes.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/** Receives PackageInstaller's in-place update result and launches Android's explicit confirmation UI. */
class VyomApkInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            }
            if (confirmation == null) {
                record(context, "failed", "Android installer confirmation intent was missing")
                return
            }
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(confirmation)
            record(context, "awaiting_owner_confirmation", "Android installer is waiting for owner confirmation")
            return
        }
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty().take(300)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                record(context, "success", "Vyom APK update installed in place")
                Log.i("VyomApkUpdate", "PackageInstaller reported in-place update success")
            }
            else -> {
                record(context, "failed", "PackageInstaller status $status: $message")
                Log.e("VyomApkUpdate", "PackageInstaller update failed with status $status: $message")
            }
        }
    }

    private fun record(context: Context, state: String, detail: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString("last_state", state)
            .putString("last_detail", detail)
            .putLong("updated_at", System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.vyom.assistant.action.APK_INSTALL_STATUS"
        private const val PREFERENCES = "vyom_factory_apk_update"
    }
}
