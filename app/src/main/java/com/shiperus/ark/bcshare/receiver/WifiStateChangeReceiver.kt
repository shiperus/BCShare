package com.shiperus.ark.bcshare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Base64
import android.util.Log
import java.nio.charset.Charset
import android.net.wifi.SupplicantState
import com.shiperus.ark.bcshare.util.MobileHotspot
import com.shiperus.ark.bcshare.util.MobileHotspot.Companion.APP_HOTSPOT_OREO_PREFIX
import com.shiperus.ark.bcshare.util.MobileHotspot.Companion.APP_HOTSPOT_PREFIX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.CoroutineContext


class WifiStateChangeReceiver(val wifiManager: WifiManager, val context: Context) :
        BroadcastReceiver() {
    interface WifiStateChangeCallback {
        fun onWifiDisabled()
        fun onConnectedWifiMatchAppSSIDPrefix()
        fun onWifiStateChanged()
    }

    var wifiStateChangeCallback: WifiStateChangeCallback? = null
    var timerTimeoutWifiConnectionFailed: Timer? = null
    override fun onReceive(p0: Context?, intent: Intent) {
        timerTimeoutWifiConnectionFailed?.cancel()
        if (wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED) {
            wifiStateChangeCallback?.onWifiDisabled()
        } else {
            val info = wifiManager.connectionInfo
            val supState = info.supplicantState
            if (isBcShareSSID(wifiManager.connectionInfo.ssid) && supState == SupplicantState.COMPLETED) {
                wifiStateChangeCallback?.onConnectedWifiMatchAppSSIDPrefix()
            } else {
                timerTimeoutWifiConnectionFailed = fixedRateTimer(
                        "delayBeforeConnectionFailed",
                        false,
                        2000,
                        1) {
                    wifiStateChangeCallback?.onWifiStateChanged()
                    timerTimeoutWifiConnectionFailed?.cancel()
                }
            }
        }
    }

    private fun isBcShareSSID(ssid: String): Boolean {
        val decodedHotspotSSID = MobileHotspot.decodeHotspotSSID(ssid)
        return decodedHotspotSSID.startsWith(APP_HOTSPOT_PREFIX) || decodedHotspotSSID.startsWith(APP_HOTSPOT_OREO_PREFIX)
    }

}