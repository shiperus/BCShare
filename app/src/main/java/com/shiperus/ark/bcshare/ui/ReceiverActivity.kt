package com.shiperus.ark.bcshare.ui

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.receiver.WifiStateChangeReceiver
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext


class ReceiverActivity :
        AppCompatActivity(),
        CoroutineScope,
        WifiStateChangeReceiver.WifiStateChangeCallback {
    override fun onWifiDisabled() {
//        Toast.makeText(this,"Wifi turned off",Toast.LENGTH_SHORT).show()
//        finish()
    }

    override fun onConnectedWifiMatchAppSSIDPrefix() {
        if (!isWifiFinallyConnected) {
            isWifiFinallyConnected = true
            launch {
                delay(4000)
                sendRequestToServer(pathUrl + CONNECT_URL)
            }
        }
    }

    override fun onWifiStateChanged() {
        launch {
            if (isWifiFinallyConnected) {
                Toast.makeText(this@ReceiverActivity, "Lost Connection To Host", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    lateinit var pathUrl: String
    val REFRESH_FILE_URL = "/getFiles"
    val DOWNLOAD_FILE_URL = "/downloadFiles/"
    val CONNECT_URL = "/connect"


    lateinit var scrollViewAvailableFiles: ScrollView
    lateinit var linearLayoutAvailableFiles: LinearLayout
    lateinit var linearLayoutProgressBar: LinearLayout
    lateinit var textViewNoAvailableFiles: TextView
    lateinit var buttonRefreshFiles: Button
    lateinit var wifiStateChangeReceiver: WifiStateChangeReceiver
    private lateinit var wifiManager: WifiManager
    override val coroutineContext: CoroutineContext = Dispatchers.Main
    var isWifiFinallyConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        pathUrl = intent.extras["formattedPathUrl"].toString()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        initView()
    }

    override fun onResume() {
        super.onResume()
        wifiStateChangeReceiver = WifiStateChangeReceiver(wifiManager, this)
        wifiStateChangeReceiver.wifiStateChangeCallback = this
        val intentFilterStateChangeReceiver = IntentFilter()
        intentFilterStateChangeReceiver.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilterStateChangeReceiver.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiStateChangeReceiver, intentFilterStateChangeReceiver)
    }

    override fun onDestroy() {
        unregisterReceiver(wifiStateChangeReceiver)
        super.onDestroy()
    }

    private fun initView() {
        scrollViewAvailableFiles = findViewById(R.id.sv_available_files)
        linearLayoutAvailableFiles = findViewById(R.id.ly_available_files)
        linearLayoutProgressBar = findViewById(R.id.ly_progress_bar)
        textViewNoAvailableFiles = findViewById(R.id.tv_no_available_files)
        buttonRefreshFiles = findViewById(R.id.btn_refresh_files)
        buttonRefreshFiles.setOnClickListener {
            refreshFilesList()
        }
    }

    private fun refreshFilesList() {
        launch {
            var response = ""
            showProgressBarLoading()
            withContext(Dispatchers.Default) {
                response = sendRequestToServer(pathUrl + REFRESH_FILE_URL)
            }
            try {
                val jsonObjectResponse = JSONObject(response)
                val jsonArrayServedFiles = jsonObjectResponse.getJSONArray("data")
                delay(2000)
                if (jsonArrayServedFiles.length() == 0) {
                    showTextViewNoAvailableFiles()
                } else {
                    linearLayoutAvailableFiles.removeAllViews()
                    for (idx in 0 until jsonArrayServedFiles.length()) {
                        val viewAvailableFileItem = layoutInflater
                                .inflate(
                                        R.layout.layout_available_file_item,
                                        linearLayoutAvailableFiles,
                                        false
                                )
                        val textViewFileName: TextView = viewAvailableFileItem.findViewById(R.id.tv_file_name)
                        val buttonDownload: Button = viewAvailableFileItem.findViewById(R.id.btn_download_file)
                        val servedFile = File(jsonArrayServedFiles.getString(idx))
                        textViewFileName.text = servedFile.name
                        buttonDownload.setOnClickListener {
                            val downloadPath = pathUrl + DOWNLOAD_FILE_URL + idx
                            val uriDownloadPath = Uri.parse(downloadPath)
                            postDownloadRequestToDM(uriDownloadPath, "")
                            Toast.makeText(this@ReceiverActivity, "Downloading " + servedFile.name, Toast.LENGTH_SHORT).show()
                        }
                        linearLayoutAvailableFiles.addView(viewAvailableFileItem)
                    }
                    showScrollViewAvailableFiles()
                }
            } catch (e: Exception) {
                Toast.makeText(
                        this@ReceiverActivity,
                        "There was a problem, please try again",
                        Toast.LENGTH_SHORT
                ).show()
                showScrollViewAvailableFiles()
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun sendRequestToServer(apiUrl: String): String = withContext(Dispatchers.Default) {
        var `is`: InputStream? = null
        try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            // Starts the query
            conn.connect()
            //                int response =
            conn.responseCode
            //                Log.d(TAG, "The response is: " + response);
            `is` = conn.inputStream
            // Convert the InputStream into a string
            readIt(`is`)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            `is`?.close()
        }
    }

    private fun postDownloadRequestToDM(uri: Uri, fileName: String): Long {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
//        request.setTitle(fileName)
//        request.setDescription("ShareThem")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(this,
                Environment.DIRECTORY_DOWNLOADS, fileName)
        return downloadManager.enqueue(request)
    }

    @Throws(IOException::class)
    private suspend fun readIt(stream: InputStream): String = withContext(Dispatchers.Default) {
        val writer = StringWriter()

        val buffer = CharArray(1024)
        try {
            val reader = BufferedReader(
                    InputStreamReader(stream, "UTF-8"))
            var n: Int
            while (true) {
                n = reader.read(buffer)
                if (n == -1)
                    break
                writer.write(buffer, 0, n)
            }
        } finally {
            stream.close()
        }
        writer.toString()
    }

    private fun showProgressBarLoading() {
        linearLayoutProgressBar.visibility = View.VISIBLE
        scrollViewAvailableFiles.visibility = View.GONE
        textViewNoAvailableFiles.visibility = View.GONE
    }

    private fun showScrollViewAvailableFiles() {
        linearLayoutProgressBar.visibility = View.GONE
        scrollViewAvailableFiles.visibility = View.VISIBLE
        textViewNoAvailableFiles.visibility = View.GONE
    }

    private fun showTextViewNoAvailableFiles() {
        linearLayoutProgressBar.visibility = View.GONE
        scrollViewAvailableFiles.visibility = View.GONE
        textViewNoAvailableFiles.visibility = View.VISIBLE
    }

}
