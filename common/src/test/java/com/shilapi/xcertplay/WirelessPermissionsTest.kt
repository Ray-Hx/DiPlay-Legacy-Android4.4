package com.shilapi.xcertplay

import android.Manifest
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
import org.junit.Assert.*
import org.junit.Test

class WirelessPermissionsTest {
    @Test fun kitKatNeedsNoRuntimePermissionForAnyWirelessMode() {
        for (mode in WirelessHotspotMode.values()) {
            assertEquals("API 19 mode=$mode", emptyList<String>(), WirelessPermissions.required(mode, 19))
        }
    }

    @Test fun android17AddsLocalNetworkToEveryModeAndOlderReleasesNeverRequestIt() {
        for (mode in WirelessHotspotMode.values()) {
            assertTrue(WirelessPermissions.required(mode, 37).contains(Manifest.permission.ACCESS_LOCAL_NETWORK))
            assertFalse(WirelessPermissions.required(mode, 36).contains(Manifest.permission.ACCESS_LOCAL_NETWORK))
        }
    }

    @Test fun existingWifiAndManualHotspotKeepLegacyPermissionExemptions() {
        for (mode in listOf(WirelessHotspotMode.EXISTING_WIFI, WirelessHotspotMode.MANUAL)) {
            assertEquals(emptyList<String>(), WirelessPermissions.required(mode, 23))
            assertEquals(emptyList<String>(), WirelessPermissions.required(mode, 29))
            assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), WirelessPermissions.required(mode, 31))
            assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), WirelessPermissions.required(mode, 33))
            assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_LOCAL_NETWORK),
                WirelessPermissions.required(mode, 37))
        }
    }

    @Test fun managedHotspotsKeepTheirLocationAndNearbyWifiRequests() {
        for (mode in listOf(WirelessHotspotMode.WIFI_P2P, WirelessHotspotMode.LOCAL_ONLY_HOTSPOT)) {
            assertEquals(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                WirelessPermissions.required(mode, 23))
            assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION), WirelessPermissions.required(mode, 31))
            assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.NEARBY_WIFI_DEVICES),
                WirelessPermissions.required(mode, 33))
        }
    }
}
