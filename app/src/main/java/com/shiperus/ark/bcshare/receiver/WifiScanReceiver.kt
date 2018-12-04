package com.shiperus.ark.bcshare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Base64
import android.util.Log
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver.Companion.APP_HOTSPOT_PREFIX
import java.nio.charset.Charset

class WifiScanReceiver(val wifiManager: WifiManager): BroadcastReceiver() {
    private var isScanFromUser: Boolean = false
    var wifiScanCallback: WifiScanCallback? = null

    override fun onReceive(p0: Context?, p1: Intent?) {
        if(!isScanFromUser)
            return
        val wifiScanResults: ArrayList<ScanResult> = ArrayList()
        for(scanResult in wifiManager.scanResults){
            val decodedHotspotSSID: String = try {
                String(
                        Base64.decode(scanResult.SSID.toByteArray(), Base64.DEFAULT),
                        Charset.defaultCharset()
                )
            } catch (e: Exception) {
                scanResult.SSID
            }
            if (!decodedHotspotSSID.startsWith(APP_HOTSPOT_PREFIX))
                continue
            wifiScanResults.add(scanResult)
        }
        wifiScanCallback?.onWifiScanComplete(wifiScanResults)
        isScanFromUser = false
    }



    interface WifiScanCallback{
        fun onWifiScanComplete(wifiScanResults: ArrayList<ScanResult>)
    }

    fun setScanFromUser(){
        this.isScanFromUser = true
    }
}