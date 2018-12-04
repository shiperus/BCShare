package com.shiperus.ark.bcshare.util

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Base64
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.*

class MobileHotspot private constructor(context: Context) {
    companion object {
        const val PORT = 8484
        private var instance: MobileHotspot? = null
        fun getInstance(context: Context):MobileHotspot{
            return if(instance == null)
                MobileHotspot(context)
            else
                instance as MobileHotspot
        }
    }

    private  var wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var wifiConfiguration: WifiConfiguration = WifiConfiguration()
    private var methodSetWifiApEnabled: Method
    private var methodGetWifiApState: Method

    private val SSID_NAME = Build.MODEL
    init {
        val wifiConfigurationClass: Class<WifiConfiguration> = WifiConfiguration::class.java
        val randomUUID = UUID.randomUUID().toString()
        val randomUUIDLastThree = randomUUID.substring(randomUUID.length - 4, randomUUID.length - 1)
        val hotspotSSID = "${WifiStateChangeReceiver.APP_HOTSPOT_PREFIX}_${SSID_NAME}_$randomUUIDLastThree"
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

    fun isWifiApEnabled(): Boolean{
        return (methodGetWifiApState.invoke(wifiManager) as Int) == 13
    }

    fun enableMobileHotspot(){
        methodSetWifiApEnabled.invoke(wifiManager, wifiConfiguration, true)
    }

    fun disableMobileHotspot(){
        methodSetWifiApEnabled.invoke(wifiManager, wifiConfiguration, false)
    }
}