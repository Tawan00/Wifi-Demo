package com.example.wifi_demo

import android.Manifest
import android.R.layout.simple_list_item_1
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifi_demo.receiver.EDCBroadcaster
import com.example.wifi_demo.utils.WifiConnectivityCallbackResult
import java.lang.StringBuilder

class DeviceList : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var wifiDeviceList: ListView? = null
    private var scanBtn: Button? = null
    private var receiverWifi: WifiReceiver? = null
    private var MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1
    private var broadcaster : EDCBroadcaster? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.device_list)

        // Example of a call to a native method
        broadcaster?.StartEDCProcess("12345678","123456789012345")


        scanBtn = findViewById(R.id.button_scan)
        wifiDeviceList = findViewById(R.id.wifiList)


        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager!!.isWifiEnabled) {
            Toast.makeText(this, "Turing WIFI ON..", Toast.LENGTH_LONG).show()
            wifiManager!!.setWifiEnabled(true)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_ACCESS_COARSE_LOCATION)
        }
        wifiManager!!.startScan()

        scanBtn!!.setOnClickListener {
            wifiManager!!.startScan()
        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

    }

    override fun onPostResume() {
        super.onPostResume()
        receiverWifi = WifiReceiver(wifiManager!!, wifiDeviceList!!)
        val intentFilter = IntentFilter()
        intentFilter.addAction(
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
        )
        registerReceiver(receiverWifi, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiverWifi)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()

        }
    }



}