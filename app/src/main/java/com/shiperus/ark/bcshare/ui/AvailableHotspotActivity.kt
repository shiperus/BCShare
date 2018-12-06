package com.shiperus.ark.bcshare.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.receiver.WifiScanReceiver
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import com.shiperus.ark.bcshare.util.MobileHotspot
import com.shiperus.ark.bcshare.util.MobileHotspot.Companion.APP_HOTSPOT_OREO_PREFIX
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.InetAddress
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

class AvailableHotspotActivity :
        AppCompatActivity(),
        WifiScanReceiver.WifiScanCallback,
        WifiStateChangeReceiver.WifiStateChangeCallback,
        CoroutineScope {
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
    val PATH_PREFIX_URL = "http://%s:%s"
    var isConnectByUser = false
    override val coroutineContext = Dispatchers.Main

    override fun onWifiScanComplete(wifiScanResults: ArrayList<ScanResult>) {
        linearLayoutAvailableWifi.removeAllViews()
        GlobalScope.launch(Dispatchers.Main) {
            delay(2000)
            if (wifiScanResults.isEmpty()) {
                showTextViewNoAvailableWifi()
            } else {
                for (scanResult in wifiScanResults) {
                    val decodedHotspotSSID = MobileHotspot.decodeHotspotSSID(scanResult.SSID)
                    val viewAvailableWifiItem = layoutInflater.inflate(
                            R.layout.layout_available_wifi_item,
                            linearLayoutAvailableWifi,
                            false
                    )
                    val textViewSSID: TextView = viewAvailableWifiItem.findViewById(R.id.tv_ssid)
                    val btnConnectWifi: Button = viewAvailableWifiItem.findViewById(R.id.btn_connect_wifi)
                    val editTextSSIDKey: EditText = viewAvailableWifiItem.findViewById(R.id.et_oreo_ssid_key)
                    editTextSSIDKey.visibility = View.GONE
                    if (decodedHotspotSSID.startsWith(APP_HOTSPOT_OREO_PREFIX)) {
                        editTextSSIDKey.visibility = View.VISIBLE
                    }
                    textViewSSID.text = if (decodedHotspotSSID.startsWith(APP_HOTSPOT_OREO_PREFIX)) {
                        decodedHotspotSSID
                    } else {
                        decodedHotspotSSID.split('_')[1]
                    }
                    btnConnectWifi.setOnClickListener {
                        val ssidKey = editTextSSIDKey.text.toString()
                        connectToWifi(scanResult.SSID, ssidKey)
                    }
                    linearLayoutAvailableWifi.addView(viewAvailableWifiItem)
                }
                showScrollViewAvailableWifi()
            }
        }
    }

    private fun connectToWifi(ssidName: String, ssidKey: String) {
        isConnectByUser = true
        showProgressBarLoading()
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "\"" + ssidName + "\""
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED
        if (ssidName.startsWith(APP_HOTSPOT_OREO_PREFIX)) {
            if (ssidKey.isEmpty()) {
                Toast.makeText(this, "Please Input Key", Toast.LENGTH_SHORT).show()
                return
            }
            wifiConfiguration.preSharedKey = "\"" + ssidKey + "\""
        } else {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }
        val networkId = wifiManager.addNetwork(wifiConfiguration)
        GlobalScope.launch(Dispatchers.Main) {
            wifiManager.isWifiEnabled = true
            delay(3000)
            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
        }
    }

    override fun onWifiDisabled() {
        if (!isWifiCheckable)
            return
        Toast.makeText(this, "Wifi disabled", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onWifiStateChanged() {
        launch {
            if (isConnectByUser) {
                Toast.makeText(this@AvailableHotspotActivity, "Can't Connect To Wifi", Toast.LENGTH_SHORT).show()
                showScrollViewAvailableWifi()
                isConnectByUser = false
            }
        }
    }

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
        intentFilterStateChangeReceiver.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
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
