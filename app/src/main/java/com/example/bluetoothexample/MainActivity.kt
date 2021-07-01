package com.example.bluetoothexample

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private var blDevice: BluetoothDevice? = null
    private var btAdapter: BluetoothAdapter? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        val bondedDevices = btAdapter!!.bondedDevices
        blDevice = bondedDevices.find {
            it.name == "HUAWEI Y7"
        }
        
        findViewById<Button>(R.id.cliente).setOnClickListener {
            BluetoothClient(blDevice!!).start()
            Log.i(TAG, "init client")
        }
        
        findViewById<Button>(R.id.servidor).setOnClickListener {
            btAdapter?.cancelDiscovery()
            BluetoothServer().start()
            Log.i(TAG, "init server")
        }
    }
    
    
    class BluetoothServer : Thread() {
        override fun run() {
            val mmServerSocket: BluetoothServerSocket? =
                BluetoothAdapter.getDefaultAdapter()?.listenUsingInsecureRfcommWithServiceRecord("BL_TEST", myUUID)
            
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                }
                catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    ConnectedThread(it).start()
                }
            }
        }
    }
    
    
    class BluetoothClient(private val device: BluetoothDevice) : Thread() {
        override fun run() {
            val mmSocket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(myUUID)
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            
            mmSocket?.let { socket ->
                socket.connect()
                
                try {
                    socket.outputStream.write("Holaa".toByteArray())
                }
                catch (e: IOException) {
                    Log.e(TAG, "Error occurred when sending data", e)
                    return
                }
            }
        }
    }
    
    class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmBuffer: ByteArray = ByteArray(1024)
        
        override fun run() {
            var numBytes: Int
            
            while (true) {
                numBytes = try {
                    mmInStream.read(mmBuffer)
                }
                catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
                finally {
                    mmSocket.close()
                }
                Log.i(TAG, String(mmBuffer, 0, numBytes))
            }
        }
    }
    
    companion object {
        private const val TAG = "BL"
        private val myUUID = UUID.fromString("67a916c9-7c2c-4eee-8140-558ce99dc6c5")
    }
}