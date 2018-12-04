package com.shiperus.ark.bcshare.server

import android.util.Log
import android.widget.Toast
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.lang.Exception
import java.net.InetAddress

class BCShareServer(port: Int) : NanoHTTPD(port) {
    var arrayListServedFiles: ArrayList<String> = ArrayList()
    var arrayListConnectedClient: ArrayList<String> = ArrayList()

    override fun serve(session: IHTTPSession): Response {
        val response: Response
        val url = session.uri
        if (url == "/getFiles") {
            response = generateServedFiles()
        } else if (url.contains("/downloadFiles/")) {
            response = try {
                val idxFile = url.split('/')[2].toInt()
                val fileInputStream = FileInputStream(arrayListServedFiles[idxFile])
                newFixedLengthResponse(
                        Response.Status.OK,
                        "application/force-download",
                        fileInputStream,
                        File(arrayListServedFiles[idxFile]).length()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                newFixedLengthResponse("No Data")
            }
        } else if (url == "/connect") {
            arrayListConnectedClient.add(session.headers["http-client-ip"].toString())
            response = newFixedLengthResponse("Connected")
        } else {
            response = newFixedLengthResponse("GGGZAAAA")
        }
        return response
    }

    private fun generateServedFiles(): Response {
        val jsonObjectData = JSONObject()
        val jsonArrayServedFiles = JSONArray()
        for (servedFile in arrayListServedFiles) {
            jsonArrayServedFiles.put(servedFile)
        }
        jsonObjectData.put("data", jsonArrayServedFiles)
        return newFixedLengthResponse(jsonObjectData.toString())
    }

    fun getConnectedClient(): ArrayList<String> {
        val arrayListConnectedClientOld = ArrayList(arrayListConnectedClient)
        arrayListConnectedClient.clear()
        var macCount = 0
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (true) {
                line = br!!.readLine()
                if (line == null)
                    break
                val splitted = line.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitted != null) {
                    val mac = splitted[3]
                    if (mac.matches("..:..:..:..:..:..".toRegex())) {
                        macCount++
                        if (arrayListConnectedClientOld.contains(splitted[0])) {
                            val inetAddress = InetAddress.getByName(splitted[0])
                            if (inetAddress.isReachable(1000))
                                arrayListConnectedClient.add(splitted[0])
                        }
                    }
                }
            }
        } catch (e: Exception) {

        }
        return arrayListConnectedClient
    }

}