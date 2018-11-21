package com.shiperus.ark.bcshare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log

class WifiScanReceiver(val wifiManager: WifiManager): BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {

        val connections: MutableList<String> = ArrayList()
        val wifiScanResults: MutableList<ScanResult>
        wifiScanResults = wifiManager.scanResults
        for (wifiScanResult in wifiScanResults){
            Log.i("SSIDLIST",wifiScanResult.SSID)
        }

//        val listConNetw = wifiManager.configuredNetworks
//        for (confNet in listConNetw) {
//            Log.i("CONFLIST",confNet.SSID)
//        }
//        wifiScanInterface?.onWifiScanComplete(wifiManager)
//        wifiScanInterface = null

    }
}