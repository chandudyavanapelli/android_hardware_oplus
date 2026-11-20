/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.doze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import java.util.concurrent.Executors
import vendor.oplus.hardware.displaypanelfeature.IDisplayPanelFeature

class AodLightSensor(private val context: Context, sensorType: String) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)!!
    private val sensor = Utils.getSensor(sensorManager, sensorType)

    private val executorService = Executors.newSingleThreadExecutor()
    private var displayPanelFeature: IDisplayPanelFeature? = null

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> disable()
                    Intent.ACTION_SCREEN_OFF -> if (Utils.isAlwaysOnEnabled(context)) enable()
                }
            }
        }

    fun init() {
        if (sensor == null) {
            return
        }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        context.registerReceiver(screenStateReceiver, filter)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.values[0]) {
            SENSOR_DARK -> setAodLightMode(AOD_LIGHT_LOW)
            SENSOR_BRIGHT -> setAodLightMode(AOD_LIGHT_HIGH)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun enable() {
        executorService.submit {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun disable() {
        executorService.submit { sensorManager.unregisterListener(this, sensor) }
    }

    private fun setAodLightMode(mode: Int) {
        executorService.submit {
            try {
                getDisplayPanelFeature()
                    ?.setDisplayPanelFeatureValue(FEATURE_AOD_LIGHT_MODE, intArrayOf(mode))
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set AOD light mode", e)
                displayPanelFeature = null
            }
        }
    }

    private fun getDisplayPanelFeature(): IDisplayPanelFeature? {
        displayPanelFeature?.let {
            return it
        }
        val binder = ServiceManager.waitForDeclaredService(SERVICE_NAME) ?: return null
        return IDisplayPanelFeature.Stub.asInterface(binder).also { displayPanelFeature = it }
    }

    companion object {
        private const val TAG = "AodLightSensor"

        private val SERVICE_NAME = "${IDisplayPanelFeature.DESCRIPTOR}/default"

        private const val FEATURE_AOD_LIGHT_MODE = 7
        private const val AOD_LIGHT_HIGH = 0
        private const val AOD_LIGHT_LOW = 1

        private const val SENSOR_DARK = 0f
        private const val SENSOR_BRIGHT = 1f
    }
}
