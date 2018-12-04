package com.shiperus.ark.bcshare.ui

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.shiperus.ark.bcshare.receiver.WifiScanReceiver
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver.Companion.APP_HOTSPOT_PREFIX
import com.shiperus.ark.bcshare.server.BCShareServer
import kotlinx.coroutines.*
import java.io.*

import java.net.*
import java.nio.charset.Charset
import java.util.*
import android.widget.Toast
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.service.BCShareService
import com.shiperus.ark.bcshare.util.MobileHotspot
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer


class HomeActivity : AppCompatActivity(){
    lateinit var buttonSend: Button
    lateinit var buttonReceive: Button
    lateinit var linearLayoutAvailableWifi: LinearLayout
    private lateinit var wifiManager: WifiManager
    private lateinit var activity: HomeActivity
    private lateinit var mobileHotspot: MobileHotspot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        mobileHotspot = MobileHotspot.getInstance(this)
        buttonSend = findViewById(R.id.btn_send)
        buttonReceive = findViewById(R.id.btn_receive)
        linearLayoutAvailableWifi = findViewById(R.id.ly_available_wifi)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        buttonSend.setOnClickListener {
            createMobileHotspot()
        }
        buttonReceive.setOnClickListener {
            mobileHotspot.disableMobileHotspot()
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
                directToAvailableWifiActivity()
            } else
                directToAvailableWifiActivity()
        }
        activity = this
    }

    private fun directToAvailableWifiActivity() {
        val intentAvailableWifiActivity = Intent(this, AvailableHotspotActivity::class.java)
        startActivity(intentAvailableWifiActivity)
    }

    private fun createMobileHotspot() {
        val intentSendActivity = Intent(this, SendActivity::class.java)
        startActivity(intentSendActivity)
    }

}
