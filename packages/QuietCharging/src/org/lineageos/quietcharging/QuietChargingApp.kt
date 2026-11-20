/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.quietcharging

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import vendor.oplus.hardware.charger.ICharger

class QuietChargingApp : Application() {
    private val handler by lazy { Handler(HandlerThread(TAG).apply { start() }.looper) }
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private var charger: ICharger? = null
    private var wireless = false

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
            handler.post(::update)
        }

    private val batteryReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                wireless =
                    intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ==
                        BatteryManager.BATTERY_PLUGGED_WIRELESS
                update()
            }
        }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            null,
            handler,
        )
    }

    private fun update() {
        if (!wireless) {
            return
        }
        try {
            val charger = getCharger() ?: return
            val silent = charger.wirelessUserSleepMode == MODE_SILENT
            // Silent mode is rejected until the charger is identified as an Oplus one.
            if (enabled != silent) {
                charger.setWirelessUserSleepMode(if (enabled) "$MODE_SILENT" else "$MODE_FASTCHG")
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to set wireless user sleep mode", e)
            charger = null
        }
    }

    private fun getCharger(): ICharger? {
        charger?.let {
            return it
        }
        val binder = ServiceManager.waitForDeclaredService(SERVICE_NAME) ?: return null
        return ICharger.Stub.asInterface(binder).also { charger = it }
    }

    companion object {
        private const val TAG = "OplusQuietCharging"

        private val SERVICE_NAME = "${ICharger.DESCRIPTOR}/default"

        private const val PREFS_NAME = "quiet_charging"
        private const val KEY_ENABLED = "enabled"

        private const val MODE_FASTCHG = 0
        private const val MODE_SILENT = 1
    }
}
