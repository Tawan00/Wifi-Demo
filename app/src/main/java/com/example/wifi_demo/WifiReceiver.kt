package com.example.wifi_demo

import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import com.example.wifi_demo.utils.WifiConnectivityCallbackResult
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder

class WifiReceiver(var wifiManager: WifiManager, wifiDeviceList: ListView) : BroadcastReceiver(),
    AdapterView.OnItemClickListener, WifiConnectivityCallbackResult {
    private val TAG: String? = "com.wifi_demo"
    private var wifiList: List<ScanResult>? = null
    private var deviceList: ArrayList<String>? = null
    private var wifiDeviceList: ListView? = null
    private var sb: StringBuilder? = null
    private lateinit var connectivityListenerCallback: WifiConnectivityCallbackResult
    private var wifiConnectivityReceiver: BroadcastReceiver? = null
    val WEP: String = "WEP"
    val WPA_WPA2_PSK: String = "WPA"
    val None: String = "None"

    private var context: Context? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context

        val action = intent!!.action
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == action) {
            sb = java.lang.StringBuilder()
            wifiList = wifiManager.scanResults
            deviceList = ArrayList()

            for (scanResult in wifiList!!) {
                sb!!.append("\n").append(scanResult.SSID).append(scanResult.BSSID)
                deviceList!!.add(scanResult.SSID.toString() + "\n" + scanResult.BSSID)
            }
            val arrayAdapter: ArrayAdapter<*> =
                ArrayAdapter<Any?>(context!!, R.layout.simple_list_item_1, deviceList!!.toArray())
            wifiDeviceList!!.adapter = arrayAdapter
            wifiDeviceList!!.onItemClickListener = this

        }

    }


    init {
        this.wifiDeviceList = wifiDeviceList

    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val wifi = wifiList?.get(p2)

        wifi?.let {
            enterPasswordDialog(it.SSID, it.BSSID)
        }
    }

    private fun enterPasswordDialog(ssid: String, bssid: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        builder.setTitle(ssid)

        val input = EditText(context)

        input.setHint("Enter Password")
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

        builder.setView(input)
        builder.setPositiveButton("Connect") { dialog, which ->
            connectWifi(ssid, input.text.toString(), WPA_WPA2_PSK, this)
        }

        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    private fun unregisterWifiConnectivityBroadcastReceiver() {
        if (wifiConnectivityReceiver != null) {
            context?.unregisterReceiver(wifiConnectivityReceiver)
        }
    }

    private fun registerWifiConnectivityBroadcastReceiver() {
        if (wifiConnectivityReceiver != null) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            context?.registerReceiver(wifiConnectivityReceiver, intentFilter)
        }
    }

    private fun wifiConnectionBroadcastReceiverInstance() {
        if (wifiConnectivityReceiver == null) {
            wifiConnectivityReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d(TAG, "Wi-Fi Connection Broadcast onReceived")
                    unregisterWifiConnectivityBroadcastReceiver()
                    connectionStatusChanged()
                }
            }
            registerWifiConnectivityBroadcastReceiver()
        } else {
            registerWifiConnectivityBroadcastReceiver()
            Log.d(TAG, "Wi-Fi Connection Broadcast Receiver Instance is already created")
        }
    }

    private fun connectionStatusChanged() {
        //Connection Success, Wi-Fi connection established
        //or Either
        //Connection Failure, Wi-Fi connection not yet established
        connectivityListenerCallback.wifiConnectionStatusChangedResult()

    }

    @SuppressLint("MissingPermission")
    private fun getWiFiConfig(networkSSID: String): WifiConfiguration? {
        val wm: WifiManager =
            context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiList = wm.configuredNetworks

        for (item in wifiList) {
            if (item.SSID != null && item.SSID == String.format("\"%s\"", networkSSID)) {
                Log.d(TAG, "Network SSID is Available in WiFiManger")
                return item
            }
        }
        Log.d(TAG, "Network SSID is Not Available in WiFiManger")
        return null
    }

    private fun connectWifi(
        networkSSID: String,
        networkPassword: String,
        networkSecurity: String,
        wifiConnectivityCallbackResult: WifiConnectivityCallbackResult
    ) {
        this.connectivityListenerCallback = wifiConnectivityCallbackResult
        if (isConnectedTo(networkSSID)) {
            //see if we are already connected to the given SSID
            connectionStatusChanged()
            getWifiInfoOf(networkSSID)?.let {
                getIPAddressFrom(it)?.let { ipAddress ->
                    Log.d("IPAddress : ", "\"Found ssid: ${it.ssid}, ip-address: $ipAddress\"")
                    Toast.makeText(
                        context,
                        "${it.ssid} is connected, ip-address: $ipAddress",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
            Log.d(TAG, "Given Network SSID is already connected : $networkSSID")
            return
        }
        wifiConnectionBroadcastReceiverInstance()
        val wm: WifiManager =
            context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var wifiConfig: WifiConfiguration? = getWiFiConfig(networkSSID)

        if (wifiConfig == null) {
            createNetworkProfile(networkSSID, networkPassword, networkSecurity)
            wifiConfig = getWiFiConfig(networkSSID)
        }

        if (wifiConfig != null) {
            wm.disconnect()
            wm.enableNetwork(wifiConfig.networkId, true)
            wm.reconnect()
            Log.d(TAG, "Initiated connection to Network SSID $networkSSID")
        } else {
            connectionStatusChanged()
            Log.d(TAG, "Connection failure to Network SSID $networkSSID")
        }
    }

    fun isConnectedTo(networkSSID: String): Boolean {
        val wm: WifiManager =
            context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wm.connectionInfo.ssid == networkSSID) {
            return true
        } else if (wm.connectionInfo.ssid == String.format("\"%s\"", networkSSID)) {
            return true
        }
        return false
    }

    private fun createNetworkProfile(
        networkSSID: String,
        networkPass: String,
        security: String
    ): Boolean {
        Log.d(TAG, "Saving Network SSID :$networkSSID Security :$security")
        val conf = WifiConfiguration()
        conf.SSID = String.format("\"%s\"", networkSSID)

        when {
            security.contains("WEP", false) -> {
                Log.d(TAG, "Configuring WEP")
                conf.wepTxKeyIndex = 0
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)

                if (networkPass.matches(Regex("^[0-9a-fA-F]+$"))) {
                    conf.wepKeys[0] = networkPass
                } else {
                    conf.wepKeys[0] = String.format("\"%s\"", networkPass)
                }

            }
            security.contains("WPA", false) -> {
                Log.d(TAG, "Configuring WPA")
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)

                conf.preSharedKey = String.format("\"%s\"", networkPass)
            }
            security.contains("None", false) -> {
                Log.d(TAG, "Configuring OPEN network")
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                conf.allowedAuthAlgorithms.clear()
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            }
        }
        val wm: WifiManager =
            context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val networkId = wm.addNetwork(conf)
        return if (networkId != -1) {
            Log.d(TAG, "Saved Network SSID to WiFiManger")
            true
        } else {
            Log.d(TAG, "Unable to Save Network SSID to WiFiManger")
            false
        }
    }

    override fun wifiConnectionStatusChangedResult() {
        for (ind in wifiList!!.indices) {
            val wifi = wifiList?.get(ind)!!

            if (isConnectedTo(wifi.SSID)) {
                wifi.capabilities = "Connected"
                return
            } else {
                wifi.capabilities = "Connected not established"
            }

        }
    }

    private fun getWifiInfoOf(networkSSID: String): WifiInfo? {
        var wifiInfo: WifiInfo? = null
        if (isConnectedTo(networkSSID)) {
            wifiConnectionInfo()?.let { info ->
                wifiInfo = info
            }
        }
        return wifiInfo
    }

    private fun getIPAddressFrom(wifiInfo: WifiInfo): String? {
        wifiInfo.let { info ->
            var infoIPAddress: Int = info.ipAddress
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                infoIPAddress = Integer.reverseBytes(infoIPAddress)
            }
            val ipByteArray = BigInteger.valueOf(infoIPAddress.toLong()).toByteArray()
            return try {
                InetAddress.getByAddress(ipByteArray).hostAddress
            } catch (e: UnknownHostException) {
                null
            }
        }
    }

    private fun wifiConnectionInfo(): WifiInfo? {
        return wifiManager.connectionInfo
    }

    private fun doDiscovery() {

    }

}