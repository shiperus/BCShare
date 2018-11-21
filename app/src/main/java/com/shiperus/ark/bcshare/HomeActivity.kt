package com.shiperus.ark.bcshare

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.shiperus.ark.bcshare.receiver.WifiScanReceiver
import kotlinx.coroutines.*

import java.lang.reflect.Method

class HomeActivity : AppCompatActivity() {
    lateinit var buttonSend: Button
    lateinit var buttonReceive: Button
    lateinit var wifiManager: WifiManager
    lateinit var method: Method
    lateinit var wifiConfiguration: WifiConfiguration
    val SSID_NAME = "BCShare"
    lateinit var wifiScanReceiver: WifiScanReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        buttonSend = findViewById(R.id.btn_send)
        buttonReceive = findViewById(R.id.btn_receive)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        buttonSend.setOnClickListener {
            initSender()
        }
        buttonReceive.setOnClickListener {
            GlobalScope.launch {
                if (!wifiManager.isWifiEnabled) {
                    wifiManager.setWifiEnabled(true)
                }
                initReceive()
            }
        }
        val wifiConfigurationClass: Class<WifiConfiguration> = WifiConfiguration::class.java
        wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = SSID_NAME
        method = wifiManager.javaClass.getDeclaredMethod(
                "setWifiApEnabled",
                wifiConfigurationClass,
                Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
    }

    override fun onResume() {
        super.onResume()
        wifiScanReceiver = WifiScanReceiver(wifiManager)
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiScanReceiver)
    }

    private fun initReceive() {
        method.invoke(wifiManager, wifiConfiguration, false)
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "\"" + SSID_NAME + "\""
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        val networkId = wifiManager.addNetwork(wifiConfiguration)
        if (!wifiManager.isWifiEnabled)
            wifiManager.setWifiEnabled(true)
        wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
    }

    private fun initSender() {
        method.invoke(wifiManager, wifiConfiguration, true)
    }
}
