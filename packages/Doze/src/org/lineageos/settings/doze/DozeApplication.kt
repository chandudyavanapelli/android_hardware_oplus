/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.doze

import android.app.Application

class DozeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AodLightSensor(this, getString(R.string.aod_light_sensor_type)).init()
    }
}
