package com.shiperus.ark.bcshare

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import java.lang.reflect.Method

class HomeActivity : AppCompatActivity() {

    lateinit var buttonSend: Button
    lateinit var buttonReceive: Button
    lateinit var wifiManager: WifiManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        buttonSend = findViewById(R.id.btn_send)
        buttonReceive = findViewById(R.id.btn_receive)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        buttonSend.setOnClickListener {
            wifiManager.setWifiEnabled(false)

//            val booleanClass: Class<Boolean> = Boolean::class.javaPrimitiveType
            val wifiConfigurationClass: Class<WifiConfiguration> = WifiConfiguration::class.java

            val wifiConfiguration: WifiConfiguration? = null
            val method: Method = wifiManager.javaClass.getDeclaredMethod(
                    "setWifiApEnabled",
                    wifiConfigurationClass,
                    Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            val bol: Boolean = method.invoke(wifiManager, wifiConfiguration , true) as Boolean
            Toast.makeText(this, " ggz $bol", Toast.LENGTH_SHORT).show()
        }
    }
}
