package com.shiperus.ark.bcshare.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.shiperus.ark.bcshare.server.BCShareServer
import com.shiperus.ark.bcshare.util.MobileHotspot
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer

class BCShareService : Service(),CoroutineScope {
    private val iBinder = BCShareServiceBinder()
    private lateinit var mobileHotspot: MobileHotspot
    private lateinit var bcShareServer: BCShareServer
    var bcShareServiceCallback: BCShareServiceCallback? = null
    var clientListUpdated: ClientListUpdated? = null
    private lateinit var fixedRateTimerHotspotAvailable: Timer
    private lateinit var fixedRateTimerConnectedClient: Timer
    override val coroutineContext = Dispatchers.Default

    interface BCShareServiceCallback {
        fun onWifiApTurnOff()
        fun onWifiApStarted()
    }

    interface ClientListUpdated {
        fun onClientListUpdated(arrayListClient: ArrayList<String>)
    }

    override fun onCreate() {
        super.onCreate()
        mobileHotspot = MobileHotspot.getInstance(this)
    }

    override fun onBind(p0: Intent?): IBinder {
        return iBinder
    }

    inner class BCShareServiceBinder : Binder() {
        fun getService(): BCShareService {
            return this@BCShareService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun startMobileHotspot() {
        launch {
            mobileHotspot.disableMobileHotspot()
            mobileHotspot.enableMobileHotspot()
            delay(2000)
            try {
                bcShareServer = BCShareServer(MobileHotspot.PORT)
                bcShareServer.start()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            detectIfMobileHotspotStillAvailable()
            detectConnectedClient()
            withContext(Dispatchers.Main){
                bcShareServiceCallback?.onWifiApStarted()
            }
        }
    }

    fun getSSIDName():String{
        return mobileHotspot.hotspotSSID
    }

    fun getHotspotKeyForOreo():String{
        return mobileHotspot.hotspotKeyForOreo
    }

    private fun detectIfMobileHotspotStillAvailable() {
        fixedRateTimerHotspotAvailable = fixedRateTimer("detectIfMobileHotspotStillAvailable", false, 4000, 1000) {
            if (!mobileHotspot.isWifiApEnabled()) {
                bcShareServiceCallback?.onWifiApTurnOff()
            }
        }
    }

    fun detectConnectedClient() {
        fixedRateTimerConnectedClient = fixedRateTimer("getConnectedClient", false, 0, 1000) {
            clientListUpdated?.onClientListUpdated(bcShareServer.getConnectedClient())
        }
    }

    fun updateServerServedFiles(arrayListServedFiles: ArrayList<String>) {
        bcShareServer.arrayListServedFiles = arrayListServedFiles
    }

    override fun onDestroy() {
        mobileHotspot.disableMobileHotspot()
        bcShareServer.stop()
        fixedRateTimerHotspotAvailable.cancel()
        fixedRateTimerConnectedClient.cancel()
        super.onDestroy()
    }

}