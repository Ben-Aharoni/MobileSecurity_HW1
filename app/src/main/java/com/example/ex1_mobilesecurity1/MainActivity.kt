package com.example.ex1_mobilesecurity1

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var btnBattery: ToggleButton
    private lateinit var btnCharging: ToggleButton
    private lateinit var btnBluetooth: ToggleButton
    private lateinit var btnWifi: ToggleButton
    private lateinit var btnContact: ToggleButton
    private lateinit var btnNext: MaterialButton

    private lateinit var wifiManager: WifiManager

    private val requiredSsid = "Sigal-2.4GHz"
    private val requiredContactName = "mom"

    private val requirements = BooleanArray(5) { false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        requestPermissionsIfNeeded()

        setupButtonLogic()
    }

    private fun initViews() {
        btnBattery = findViewById(R.id.btnBattery)
        btnCharging = findViewById(R.id.btnCharging)
        btnBluetooth = findViewById(R.id.btnBluetooth)
        btnWifi = findViewById(R.id.btnWifi)
        btnContact = findViewById(R.id.btnContact)
        btnNext = findViewById(R.id.btnNext)
    }

    private fun setupButtonLogic() {
        btnBattery.setOnClickListener {
            val ok = checkBatteryLevel()
            btnBattery.isChecked = ok
            if (!ok) showToast("Battery level is too low")
            requirements[0] = ok
            checkAllRequirements()
        }

        btnCharging.setOnClickListener {
            val ok = checkCharging()
            btnCharging.isChecked = ok
            if (!ok) showToast("Device not charging")
            requirements[1] = ok
            checkAllRequirements()
        }

        btnBluetooth.setOnClickListener {
            val ok = checkBluetooth()
            btnBluetooth.isChecked = ok
            if (!ok) showToast("Bluetooth is OFF")
            requirements[2] = ok
            checkAllRequirements()
        }

        btnWifi.setOnClickListener {
            scanWifi()
        }

        btnContact.setOnClickListener {
            val ok = checkContact()
            btnContact.isChecked = ok
            if (!ok) showToast("Contact not found")
            requirements[4] = ok
            checkAllRequirements()
        }

        btnNext.setOnClickListener {
            startActivity(Intent(this, AccessGranted::class.java))
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        ActivityCompat.requestPermissions(this, permissions, 101)
    }

    private fun checkBatteryLevel(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return level > 30
    }

    private fun checkCharging(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun checkBluetooth(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    adapter?.isEnabled == true
        } else {
            adapter?.isEnabled == true
        }
    }

    private fun scanWifi() {
        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val wifiState = Manifest.permission.ACCESS_WIFI_STATE

        val hasPermissions = ContextCompat.checkSelfPermission(this, fineLocation) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, wifiState) == PackageManager.PERMISSION_GRANTED

        if (!hasPermissions) {
            showToast("Required permissions missing to scan Wi-Fi")
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                unregisterReceiver(this)
                val ssidFound = try {
                    wifiManager.scanResults.any { it.SSID == requiredSsid }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    showToast("Security error while reading Wi-Fi scan results")
                    false
                }

                btnWifi.isChecked = ssidFound
                requirements[3] = ssidFound
                if (!ssidFound) showToast("Wi-Fi '$requiredSsid' not found")
                checkAllRequirements()
            }
        }

        try {
            registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiManager.startScan()
        } catch (e: SecurityException) {
            e.printStackTrace()
            showToast("Couldn't start Wi-Fi scan — check permissions")
        }
    }

    private fun checkContact(): Boolean {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                if (it.getString(0).equals(requiredContactName, ignoreCase = true)) return true
            }
        }
        return false
    }

    private fun checkAllRequirements() {
        btnNext.isEnabled = requirements.all { it }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
