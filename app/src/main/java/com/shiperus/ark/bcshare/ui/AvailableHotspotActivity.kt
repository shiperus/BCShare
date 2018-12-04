package com.shiperus.ark.bcshare.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.receiver.WifiScanReceiver
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver.Companion.APP_HOTSPOT_PREFIX
import com.shiperus.ark.bcshare.util.MobileHotspot
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.charset.Charset

class AvailableHotspotActivity :
        AppCompatActivity(),
        WifiScanReceiver.WifiScanCallback,
        WifiStateChangeReceiver.WifiStateChangeCallback {
    lateinit var scrollViewAvailableWifi: ScrollView
    lateinit var linearLayoutAvailableWifi: LinearLayout
    lateinit var linearLayoutProgressBar: LinearLayout
    lateinit var textViewNoAvailableWifi: TextView
    lateinit var buttonSearchWifi: Button
    lateinit var mobileHotspot: MobileHotspot
    lateinit var wifiScanReceiver: WifiScanReceiver
    lateinit var wifiStateChangeReceiver: WifiStateChangeReceiver
    private lateinit var wifiManager: WifiManager
    private var isWifiCheckable: Boolean = false

    override fun onWifiScanComplete(wifiScanResults: ArrayList<ScanResult>) {
        linearLayoutAvailableWifi.removeAllViews()
        GlobalScope.launch(Dispatchers.Main) {
            delay(2000)
            if (wifiScanResults.isEmpty()) {
                showTextViewNoAvailableWifi()
            } else {
                for (scanResult in wifiScanResults) {
                    val decodedHotspotSSID: String = try {
                        String(
                                Base64.decode(scanResult.SSID.toByteArray(), Base64.DEFAULT),
                                Charset.defaultCharset()
                        )
                    } catch (e: Exception) {
                        scanResult.SSID
                    }
                    val viewAvailableWifiItem = layoutInflater.inflate(
                            R.layout.layout_available_wifi_item,
                            linearLayoutAvailableWifi,
                            false
                    )
                    val textViewSSID: TextView = viewAvailableWifiItem.findViewById(R.id.tv_ssid)
                    val btnConnectWifi: Button = viewAvailableWifiItem.findViewById(R.id.btn_connect_wifi)
                    textViewSSID.text = decodedHotspotSSID.split('_')[1]
                    btnConnectWifi.setOnClickListener {
                        isConnectByUser = true
                        showProgressBarLoading()
                        val wifiConfiguration = WifiConfiguration()
                        wifiConfiguration.SSID = "\"" + scanResult.SSID + "\""
                        wifiConfiguration.status = WifiConfiguration.Status.ENABLED
                        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        val networkId = wifiManager.addNetwork(wifiConfiguration)
                        GlobalScope.launch(Dispatchers.Main) {
                            wifiManager.isWifiEnabled = true
                            delay(3000)
                            wifiManager.disconnect()
                            wifiManager.enableNetwork(networkId, true)
                            wifiManager.reconnect()
                        }
                    }
                    linearLayoutAvailableWifi.addView(viewAvailableWifiItem)
                }
                showScrollViewAvailableWifi()
            }
        }
    }

    override fun onWifiDisabled() {
        if (!isWifiCheckable)
            return
        Toast.makeText(this, "Wifi disabled", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onWifiStateChanged() {}

    val PATH_PREFIX_URL = "http://%s:%s"
    var isConnectByUser = false

    override fun onConnectedWifiMatchAppSSIDPrefix() {
        if (isConnectByUser) {
            GlobalScope.launch(Dispatchers.Main) {
                val wifiManagerNew = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddressByteArray = convertToBytes(wifiManagerNew.dhcpInfo.serverAddress)
                val ipAddressString = InetAddress.getByAddress(ipAddressByteArray).hostAddress.replace("/", "")
                val formattedPathUrl = String.format(PATH_PREFIX_URL, ipAddressString, MobileHotspot.PORT)
                directToReceiveFileActivity(formattedPathUrl)
                isConnectByUser = false
            }
        }
    }

    private fun directToReceiveFileActivity(formattedPathUrl: String) {
        val receiverActivityIntent = Intent(this, ReceiverActivity::class.java)
        receiverActivityIntent.putExtra("formattedPathUrl", formattedPathUrl)
        startActivity(receiverActivityIntent)
        finish()
    }

    private fun convertToBytes(hostAddress: Int): ByteArray {
        return byteArrayOf((0xff and hostAddress).toByte(), (0xff and (hostAddress shr 8)).toByte(), (0xff and (hostAddress shr 16)).toByte(), (0xff and (hostAddress shr 24)).toByte())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_available_hotspot)
        mobileHotspot = MobileHotspot.getInstance(this)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        initView()
        GlobalScope.launch {
            delay(2000)
            isWifiCheckable = true
        }
    }

    override fun onResume() {
        super.onResume()
        wifiScanReceiver = WifiScanReceiver(wifiManager)
        wifiScanReceiver.wifiScanCallback = this
        val intentFilterScanReceiver = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilterScanReceiver)
        wifiStateChangeReceiver = WifiStateChangeReceiver(wifiManager, this)
        wifiStateChangeReceiver.wifiStateChangeCallback = this
        val intentFilterStateChangeReceiver = IntentFilter()
        intentFilterStateChangeReceiver.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilterStateChangeReceiver.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiStateChangeReceiver, intentFilterStateChangeReceiver)
    }

    override fun onDestroy() {
        unregisterReceiver(wifiScanReceiver)
        unregisterReceiver(wifiStateChangeReceiver)
        super.onDestroy()
    }

    private fun initView() {
        scrollViewAvailableWifi = findViewById(R.id.sv_available_wifi_list)
        linearLayoutAvailableWifi = findViewById(R.id.ly_available_wifi_list)
        linearLayoutProgressBar = findViewById(R.id.ly_progress_bar)
        textViewNoAvailableWifi = findViewById(R.id.tv_no_available_wifi)
        buttonSearchWifi = findViewById(R.id.btn_search_wifi)
        buttonSearchWifi.setOnClickListener {
            searchAvailableBCShareHotspot()
        }
    }

    private fun searchAvailableBCShareHotspot() {
        wifiScanReceiver.setScanFromUser()
        showProgressBarLoading()
        wifiManager.startScan()
    }

    private fun showProgressBarLoading() {
        linearLayoutProgressBar.visibility = View.VISIBLE
        scrollViewAvailableWifi.visibility = View.GONE
        textViewNoAvailableWifi.visibility = View.GONE
    }

    private fun showScrollViewAvailableWifi() {
        linearLayoutProgressBar.visibility = View.GONE
        scrollViewAvailableWifi.visibility = View.VISIBLE
        textViewNoAvailableWifi.visibility = View.GONE
    }

    private fun showTextViewNoAvailableWifi() {
        linearLayoutProgressBar.visibility = View.GONE
        scrollViewAvailableWifi.visibility = View.GONE
        textViewNoAvailableWifi.visibility = View.VISIBLE
    }

}
