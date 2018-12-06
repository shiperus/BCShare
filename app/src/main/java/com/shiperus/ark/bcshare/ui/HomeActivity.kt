package com.shiperus.ark.bcshare.ui

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import java.nio.charset.Charset
import java.util.*
import android.widget.Toast
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.service.BCShareService
import com.shiperus.ark.bcshare.util.MobileHotspot
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer


class HomeActivity : AppCompatActivity() {
    lateinit var buttonSend: Button
    lateinit var buttonReceive: Button
    lateinit var linearLayoutAvailableWifi: LinearLayout
    private lateinit var wifiManager: WifiManager
    private lateinit var activity: HomeActivity
    private lateinit var mobileHotspot: MobileHotspot
    lateinit var intentSendActivity: Intent

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 21)
                }else{
                    directToAvailableWifiActivity()
                }
            } else {
                directToAvailableWifiActivity()
            }
        }
        activity = this
    }

    private fun directToAvailableWifiActivity() {
        val intentAvailableWifiActivity = Intent(this, AvailableHotspotActivity::class.java)
        mobileHotspot.disableMobileHotspot()
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
            startActivity(intentAvailableWifiActivity)
        } else
            startActivity(intentAvailableWifiActivity)
    }

    private fun createMobileHotspot() {
        intentSendActivity = Intent(this, SendActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isLocationEnabled()) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 20)
                } else {
                    startActivity(intentSendActivity)
                }
            } else {
                Toast.makeText(this, "Please Enable Location", Toast.LENGTH_SHORT).show()
            }
        } else {
            startActivity(intentSendActivity)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationMode: Int = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            20 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intentSendActivity)
                }
            }
            21 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    directToAvailableWifiActivity()
                }
            }



        }
    }

}
