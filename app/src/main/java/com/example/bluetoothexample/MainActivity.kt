package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var btAdapter: BluetoothAdapter? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = mBluetoothAdapter.bondedDevices
        
        val myDevice = pairedDevices.filter {
            it.name == "HUAWEI Y7"
        }
        
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        
        findViewById<Button>(R.id.cliente).setOnClickListener {
            BluetoothClient(myDevice[0]).start()
            Log.i(TAG, "looking for devices")
        }
        
        findViewById<Button>(R.id.servidor).setOnClickListener {
            BluetoothServerController(this).start()
            Log.i(TAG, "init server")
        }
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
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    break
                }
                
                if (!this.cancelled && socket != null) {
                    Log.i("server", "Connecting")
                    BluetoothServer(activity, socket).start()
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
        
        override fun run() {
            val buffer = ByteArray(1024)
            Log.i(TAG, inputStream.available().toString())
            
            try {
                Log.i("server", "Reading")
                val bytes = inputStream.read(buffer)
                val text = String(buffer, 0, bytes)
                
                Log.i("server", "Message received $text")
                
                activity.runOnUiThread {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e("client", "Cannot read data", e)
            }
        }
    }
    
    private inner class BluetoothClient(device: BluetoothDevice) : Thread() {
        private val socket = device.createRfcommSocketToServiceRecord(myUUID)
        
        override fun run() {
            btAdapter!!.cancelDiscovery()
            Log.i("client", "Connecting")
            socket.connect()
            
            val outputStream = socket.outputStream
            
            Log.i("client", "Sending")
            try {
                outputStream.write("Hello".toByteArray())
                outputStream.flush()
                
                Log.i("client", "Sent message")
            } catch (e: Exception) {
                Log.e("client", "Cannot send", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "BL"
        private val myUUID = UUID.fromString("67a916c9-7c2c-4eee-8140-558ce99dc6c5")
    }
}