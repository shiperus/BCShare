package com.shiperus.ark.bcshare.util

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.util.Base64
import android.util.Log
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.*

class MobileHotspot private constructor(context: Context) {
    companion object {
        const val PORT = 8484
        val APP_HOTSPOT_PREFIX = "BC" + Character.toString(26.toChar())
        val APP_HOTSPOT_OREO_PREFIX = "AndroidShare"
        private var instance: MobileHotspot? = null
        fun getInstance(context: Context): MobileHotspot {
            return if (instance == null)
                MobileHotspot(context)
            else
                instance as MobileHotspot
        }

        fun decodeHotspotSSID(ssid:String): String{
            val ssidFiltered = ssid.replace("\"","")
            return try {
                if (ssidFiltered.startsWith(APP_HOTSPOT_OREO_PREFIX)) {
                    ssidFiltered
                } else
                    String(
                            Base64.decode(ssidFiltered.toByteArray(), Base64.DEFAULT),
                            Charset.defaultCharset()
                    )
            } catch (e: Exception) {
                ssidFiltered
            }
        }
    }

    private var wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var wifiConfiguration: WifiConfiguration = WifiConfiguration()
    private var methodSetWifiApEnabled: Method
    private var methodGetWifiApState: Method
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val SSID_NAME = Build.MODEL
    var hotspotSSID: String
    var hotspotKeyForOreo: String

    init {
        val wifiConfigurationClass: Class<WifiConfiguration> = WifiConfiguration::class.java
        val randomUUID = UUID.randomUUID().toString()
        val randomUUIDLastThree = randomUUID.substring(randomUUID.length - 4, randomUUID.length - 1)
        hotspotSSID = "${APP_HOTSPOT_PREFIX}_${SSID_NAME}_$randomUUIDLastThree"
        hotspotKeyForOreo=""
        val base64HotspotSSID = Base64.encode(hotspotSSID.toByteArray(), Base64.DEFAULT)
        wifiConfiguration.SSID = String(base64HotspotSSID, Charset.defaultCharset())
        methodSetWifiApEnabled = wifiManager.javaClass.getDeclaredMethod(
                "setWifiApEnabled",
                wifiConfigurationClass,
                Boolean::class.javaPrimitiveType
        )
        methodSetWifiApEnabled.isAccessible = true

        methodGetWifiApState = wifiManager.javaClass.getDeclaredMethod(
                "getWifiApState"
        )
        methodGetWifiApState.isAccessible = true
    }

    fun isWifiApEnabled(): Boolean {
        return (methodGetWifiApState.invoke(wifiManager) as Int) == 13
    }

    fun enableMobileHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                    super.onStarted(reservation)
                    hotspotReservation = reservation
                    hotspotSSID = hotspotReservation?.wifiConfiguration?.SSID.toString()
                    hotspotKeyForOreo = hotspotReservation?.wifiConfiguration?.preSharedKey.toString()
//                    hotspotReservation?.wifiConfiguration?.wa
                }
            }, null)
        else
            methodSetWifiApEnabled.invoke(wifiManager, wifiConfiguration, true)
    }

    fun disableMobileHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            hotspotReservation?.close()
        else
            methodSetWifiApEnabled.invoke(wifiManager, wifiConfiguration, false)
    }

}