package com.example.wifi_demo

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class WifiServer : AppCompatActivity(), View.OnClickListener {
    private var serverSocket: ServerSocket? = null
    private var tempClientSocket: Socket? = null
    private var serverThread: Thread? = null
    private var msgList: LinearLayout? = null
    private var handler: Handler? = null
    private var edMessage: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        title = "Server"
        handler = Handler()
        msgList = findViewById(R.id.msgList)
        edMessage = findViewById(R.id.edMessage)
    }

    fun textView(message: String?, color: Int): TextView {
        var message = message
        if (null == message || message.trim { it <= ' ' }.isEmpty()) {
            message = "<Empty Message>"
        }
        val m = "$message [$time]"
        val tv = TextView(this)
        tv.setTextColor(color)
        tv.text = m
        tv.textSize = 20f
        tv.setPadding(0, 5, 0, 0)
        return tv
    }

    fun showMessage(message: String?, color: Int) {
        handler!!.post { msgList!!.addView(textView(message, color)) }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.start_server) {
            removeAllViews()
            showMessage("Server Started.", Color.BLACK)
            serverThread = Thread(ServerThread())
            serverThread!!.start()
            return
        }
        if (view.id == R.id.send_data) {
            val msg = edMessage!!.text.toString().trim { it <= ' ' }
            showMessage("Server : $msg", Color.BLUE)
            sendMessage(msg)
        }
    }

    private fun removeAllViews() {
        handler!!.post { msgList!!.removeAllViews() }
    }

    private fun hideStartServerBtn() {
        handler!!.post {
            findViewById<View>(R.id.start_server).visibility = View.GONE
        }
    }

    private fun sendMessage(message: String) {
        try {
            if (null != tempClientSocket) {
                Thread {
                    var out: PrintWriter? = null
                    try {
                        out = PrintWriter(
                            BufferedWriter(
                                OutputStreamWriter(tempClientSocket?.getOutputStream())
                            ),
                            true
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    out!!.println(message)
                }.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal inner class ServerThread : Runnable {
        override fun run() {
            var socket: Socket?
            try {
                hideStartServerBtn()
                serverSocket = ServerSocket(SERVER_PORT)
            } catch (e: IOException) {
                e.printStackTrace()
                showMessage("Error Starting Server : " + e.message, Color.RED)
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket = serverSocket!!.accept()
                        val commThread: CommunicationThread = CommunicationThread(socket)
                        Thread(commThread).start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        showMessage("Error Communicating to Client :" + e.message, Color.RED)
                    }
                }
            }
        }
    }

    internal inner class CommunicationThread(private val clientSocket: Socket) :
        Runnable {
        private var input: BufferedReader? = null
        override fun run() {
            while (!Thread.currentThread().isInterrupted) try {
                var read = input!!.readLine()
                if (null == read || "Disconnect".contentEquals(read)) {
                    val interrupted = Thread.interrupted()
                    read = "Client Disconnected: $interrupted"
                    showMessage("Client : $read", Color.DKGRAY)
                    break
                }
                showMessage("Client : $read", Color.DKGRAY)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            tempClientSocket = clientSocket
            try {
                input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            } catch (e: IOException) {
                e.printStackTrace()
                showMessage("Error Connecting to Client!!", Color.RED)
            }
            showMessage("Connected to Client!!", Color.DKGRAY)
        }
    }

    val time: String
        get() = SimpleDateFormat("HH:mm:ss").format(Date())

    override fun onDestroy() {
        super.onDestroy()
        if (null != serverThread) {
            sendMessage("Disconnect")
            serverThread!!.interrupt()
            serverThread = null
        }
    }

    companion object {
        const val SERVER_PORT = 5000
    }
}