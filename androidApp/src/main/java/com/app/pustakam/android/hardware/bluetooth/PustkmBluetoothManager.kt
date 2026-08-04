package com.app.pustakam.android.hardware.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class PustkmBluetoothManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private var leScanner: BluetoothLeScanner? = null
    val foundDevices: SnapshotStateList<BluetoothDevice> = mutableStateListOf()
    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                device?.let {
                    if (!foundDevices.contains(it)) foundDevices.add(it)
                }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let {
                if (!foundDevices.contains(it)) foundDevices.add(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        foundDevices.clear()
        // Start classic scan
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(classicReceiver, filter)
        bluetoothAdapter.startDiscovery()
        // Start BLE scan if supported
        if (bluetoothAdapter.isMultipleAdvertisementSupported) {
            leScanner = bluetoothAdapter.bluetoothLeScanner
            leScanner?.startScan(leScanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try { context.unregisterReceiver(classicReceiver) } catch (_: Exception) {}
        bluetoothAdapter.cancelDiscovery()
        leScanner?.stopScan(leScanCallback)
    }
}

