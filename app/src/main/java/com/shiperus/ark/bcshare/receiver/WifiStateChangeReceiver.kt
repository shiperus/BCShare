package com.shiperus.ark.bcshare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Base64
import android.util.Log
import java.nio.charset.Charset
import android.net.wifi.SupplicantState


class WifiStateChangeReceiver(val wifiManager: WifiManager, val context: Context) : BroadcastReceiver() {
    //    private var isScanFromUser: Boolean = false
//    var wifiScanCallback: WifiScanCallback? = null
    interface WifiStateChangeCallback {
        fun onWifiDisabled()
        fun onConnectedWifiMatchAppSSIDPrefix()
        fun onWifiStateChanged()
    }

    companion object {
        val APP_HOTSPOT_PREFIX = "BC" + Character.toString(26.toChar())
    }

    var wifiStateChangeCallback: WifiStateChangeCallback? = null

    override fun onReceive(p0: Context?, p1: Intent) {
        if (wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED) {
            wifiStateChangeCallback?.onWifiDisabled()
        } else {
            val info = wifiManager.connectionInfo
            val supState = info.supplicantState
            if (isBcShareSSID(wifiManager.connectionInfo.ssid) && supState == SupplicantState.COMPLETED) {
                wifiStateChangeCallback?.onConnectedWifiMatchAppSSIDPrefix()
            }else{
                wifiStateChangeCallback?.onWifiStateChanged()
            }
        }
    }

    private fun isBcShareSSID(ssid: String): Boolean {
        val decodedHotspotSSID: String = try {
            String(
                    Base64.decode(ssid.toByteArray(), Base64.DEFAULT),
                    Charset.defaultCharset()
            )
        } catch (e: Exception) {
            ssid
        }
        return decodedHotspotSSID.startsWith(APP_HOTSPOT_PREFIX)
    }

//    fun setScanFromUser(){
//        this.isScanFromUser = true
//    }
}