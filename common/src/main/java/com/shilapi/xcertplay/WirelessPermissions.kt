package com.shilapi.xcertplay

import android.Manifest
import android.os.Build
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode

/** Upstream #425; keep KitKat install-time permissions and Legacy manual-hotspot behavior. */
internal object WirelessPermissions {
    fun required(wirelessHotspotMode: WirelessHotspotMode, sdkInt: Int): List<String> {
        val needsWifiRuntimePermission = wirelessHotspotMode != WirelessHotspotMode.MANUAL &&
            wirelessHotspotMode != WirelessHotspotMode.EXISTING_WIFI
        return when {
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> buildList {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                if (needsWifiRuntimePermission) add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            sdkInt >= Build.VERSION_CODES.S -> buildList {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                if (needsWifiRuntimePermission) {
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            sdkInt >= Build.VERSION_CODES.M && needsWifiRuntimePermission -> listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            else -> emptyList()
        }.let { permissions ->
            if (sdkInt >= 37) permissions + Manifest.permission.ACCESS_LOCAL_NETWORK else permissions
        }
    }
}
