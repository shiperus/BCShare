package com.shiperus.ark.bcshare.ui

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.*
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.shiperus.ark.bcshare.R
import com.shiperus.ark.bcshare.service.BCShareService
import com.shiperus.ark.bcshare.util.MobileHotspot
import java.io.File
import kotlin.collections.ArrayList

class SendActivity :
        AppCompatActivity(),
        BCShareService.BCShareServiceCallback,
        DialogSelectionListener,
        BCShareService.ClientListUpdated{
    override fun onClientListUpdated(arrayListClient: ArrayList<String>) {
        runOnUiThread {
            textViewTotalConnectedDevice.text = "${arrayListClient.size} devices connected"
        }
    }

    private lateinit var mobileHotspot: MobileHotspot
    var bcShareService: BCShareService? = null
    private lateinit var buttonAddFiles: Button
    private lateinit var textViewNoFileAdded: TextView
    private lateinit var textViewTotalConnectedDevice: TextView
    private lateinit var scrollViewAddedFileList: ScrollView
    private lateinit var linearLayoutAddedFile: LinearLayout
    private lateinit var addedFileList: ArrayList<String>

    override fun onSelectedFilePaths(files: Array<String>) {
        if (!files.isEmpty()) {
            addedFileList.addAll(files)
            textViewNoFileAdded.visibility = View.GONE
            generateAddedFileLayout()
            scrollViewAddedFileList.visibility = View.VISIBLE
        }
    }

    private fun generateAddedFileLayout() {
        linearLayoutAddedFile.removeAllViews()
        if (addedFileList.isEmpty()) {
            textViewNoFileAdded.visibility = View.VISIBLE
            scrollViewAddedFileList.visibility = View.GONE
            return
        }
        for (filePath in addedFileList) {
            val viewAddedFile = layoutInflater
                    .inflate(R.layout.layout_added_file_item, linearLayoutAddedFile, false)
            val addedFile = File(filePath)
            val textViewFileName: TextView = viewAddedFile.findViewById(R.id.tv_file_name)
            val buttonRemoveFile: Button = viewAddedFile.findViewById(R.id.btn_remove_file)
            textViewFileName.text = addedFile.name
            buttonRemoveFile.setOnClickListener {
                addedFileList.remove(filePath)
                generateAddedFileLayout()
            }
            linearLayoutAddedFile.addView(viewAddedFile)
        }
        bcShareService?.updateServerServedFiles(addedFileList)
    }

    override fun onWifiApTurnOff() {
        runOnUiThread {
            if (!isFinishing) {
                Toast.makeText(this, "Mobile Hotspot Turn Off", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            bcShareService = null
        }

        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            bcShareService = (binder as BCShareService.BCShareServiceBinder).getService()
            bcShareService?.bcShareServiceCallback = this@SendActivity
            bcShareService?.clientListUpdated = this@SendActivity
            bcShareService?.startMobileHotspot()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        mobileHotspot = MobileHotspot.getInstance(this)
        addedFileList = ArrayList()
        initView()
        bindService(Intent(this, BCShareService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initView() {
        buttonAddFiles = findViewById(R.id.btn_add_file_to_be_served)
        textViewNoFileAdded = findViewById(R.id.tv_no_file_added)
        textViewTotalConnectedDevice = findViewById(R.id.tv_total_connected_device)
        scrollViewAddedFileList = findViewById(R.id.sv_added_file_list)
        linearLayoutAddedFile = findViewById(R.id.ly_added_file_list)
        buttonAddFiles.setOnClickListener {
            showFilePickerDialog()
        }
    }

    private fun showFilePickerDialog() {
        val dialogProperties = DialogProperties()
        dialogProperties.selection_mode = DialogConfigs.MULTI_MODE
        dialogProperties.selection_type = DialogConfigs.FILE_SELECT
        dialogProperties.root = Environment.getExternalStorageDirectory()
        dialogProperties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        dialogProperties.offset = File(DialogConfigs.DEFAULT_DIR)
        dialogProperties.extensions = null
        val filePickerDialog = FilePickerDialog(this, dialogProperties)
        filePickerDialog.setTitle("Select File")
        filePickerDialog.setDialogSelectionListener(this)
        filePickerDialog.show()
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onBackPressed() {
        showDialogStopSending()
    }

    private fun showDialogStopSending() {
        val dialog = AlertDialog.Builder(this)
        val dialogInterface = object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface?, which: Int) {
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        finish()
                    }
                }
            }
        }
        dialog.setTitle("Alert")
        dialog.setMessage("Are you sure want to stop send?")
        dialog.setPositiveButton("Yes", dialogInterface)
        dialog.setNegativeButton("No", dialogInterface)
        dialog.show()
    }
}
