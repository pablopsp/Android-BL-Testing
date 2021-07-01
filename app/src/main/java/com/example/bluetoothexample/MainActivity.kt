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
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            BluetoothAdapter.getDefaultAdapter()?.listenUsingInsecureRfcommWithServiceRecord("BL_TEST", myUUID)
        }
        
        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
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
        
        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            }
            catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
    
    
    class BluetoothClient(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(myUUID)
        }
        
        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            
            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                ConnectedThread(socket).write("Holaaa".toByteArray())
            }
        }
        
        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            }
            catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
    
    class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        
        override fun run() {
            var numBytes: Int // bytes returned from read()
            
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
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
        
        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            }
            catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                return
            }
        }
        
        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            }
            catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "BL"
        private val myUUID = UUID.fromString("67a916c9-7c2c-4eee-8140-558ce99dc6c5")
    }
}