package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var blDevice: BluetoothDevice
    private var btAdapter: BluetoothAdapter? = null
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    Log.i(TAG, deviceName.toString())
                    if (device != null && deviceName.equals("HUAWEI Y7")) {
                        Log.i(TAG, "encontrado device")
                        blDevice = device
                        btAdapter?.cancelDiscovery()
                    }
                    
                    Log.i("Bl", deviceName.toString())
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        btAdapter?.startDiscovery()
        
        findViewById<Button>(R.id.cliente).setOnClickListener {
            if (this::blDevice.isInitialized) {
                BluetoothClient(blDevice).start()
                Log.i(TAG, "init client")
            }
        }
        
        findViewById<Button>(R.id.servidor).setOnClickListener {
            btAdapter?.cancelDiscovery()
            BluetoothServerController(this).start()
            Log.i(TAG, "init server")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
    
    private inner class BluetoothServerController(private val activity: MainActivity) : Thread() {
        private var cancelled: Boolean
        private val serverSocket: BluetoothServerSocket?
        
        init {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null) {
                this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("BL_TEST", myUUID) // 1
                this.cancelled = false
            } else {
                this.serverSocket = null
                this.cancelled = true
            }
            
        }
        
        override fun run() {
            var socket: BluetoothSocket
            
            while (true) {
                if (this.cancelled) {
                    break
                }
                
                try {
                    socket = serverSocket!!.accept()  // 2
                } catch (e: IOException) {
                    break
                }
                
                if (!this.cancelled && socket != null) {
                    Log.i("server", "Connecting")
                    BluetoothServer(activity, socket).start() // 3
                }
            }
        }
        
        fun cancel() {
            this.cancelled = true
            this.serverSocket!!.close()
        }
    }
    
    private inner class BluetoothServer(private val activity: MainActivity, private val socket: BluetoothSocket) :
        Thread() {
        private val inputStream = this.socket.inputStream
        private val outputStream = this.socket.outputStream
        
        override fun run() {
            try {
                val available = inputStream.available()
                val bytes = ByteArray(available)
                
                Log.i("server", "Reading")
                inputStream.read(bytes, 0, available)
                val text = String(bytes)
                
                Log.i("server", "Message received $text")
                
                activity.runOnUiThread {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("client", "Cannot read data", e)
            } finally {
                inputStream.close()
                outputStream.close()
                this.socket.close()
            }
        }
    }
    
    private inner class BluetoothClient(device: BluetoothDevice) : Thread() {
        private val socket = device.createRfcommSocketToServiceRecord(myUUID)
        
        override fun run() {
            Log.i("client", "Connecting")
            this.socket.connect()
            
            val inputStream = this.socket.inputStream
            val outputStream = this.socket.outputStream
            
            Log.i("client", "Sending")
            try {
                outputStream.write("Holaa".toByteArray())
                outputStream.flush()
                Log.i("client", "Sent message")
            } catch (e: Exception) {
                Log.e("client", "Cannot send", e)
            } finally {
                outputStream.close()
                inputStream.close()
                this.socket.close()
            }
        }
    }
    
    companion object {
        private const val TAG = "BL"
        private val myUUID = UUID.fromString("67a916c9-7c2c-4eee-8140-558ce99dc6c5")
    }
}