/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.quietcharging

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuietChargingTileService : TileService() {
    private val app by lazy { application as QuietChargingApp }

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        app.enabled = !app.enabled
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (app.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
