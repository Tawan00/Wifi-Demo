package com.example.wifi_demo

class WifiState {
    // Constants that indicate the current connection state
    val STATE_NONE = 0 // we're doing nothing

    val STATE_LISTEN = 1 // now listening for incoming connections

    val STATE_CONNECTING = 2 // now initiating an outgoing connection

    val STATE_CONNECTED = 3 // now connected to a remote device

    val STATE_NULL = -1 // now service is null


    val MESSAGE_STATE_CHANGE = 1
    val MESSAGE_READ = 2
    val MESSAGE_WRITE = 3
    val MESSAGE_DEVICE_NAME = 4
    val MESSAGE_TOAST = 5

    // Intent request codes
    val REQUEST_CONNECT_DEVICE = 384
    val REQUEST_ENABLE_BT = 385

    // Key names received from the BluetoothChatService Handler
    val DEVICE_NAME = "device_name"
    val DEVICE_ADDRESS = "device_address"
    val TOAST = "toast"

    val DEVICE_ANDROID = true
    val DEVICE_OTHER = false

    // Return Intent extra
    var EXTRA_DEVICE_ADDRESS = "device_address"

}