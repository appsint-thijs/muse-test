package nl.appsint.musetest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

abstract class PermissionActivity: AppCompatActivity() {
    protected abstract fun onPermissionsInitialized()

    private val requestEnableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onBluetoothState(result.resultCode == RESULT_OK)
    }

    private val requestEnableLocation = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onPermissionState(result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionState()
    }



    // The following functions are called in order.

    private fun checkPermissionState() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSIONS)
        } else {
            onPermissionState(granted = true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS) {
            onPermissionState(permissions.isNotEmpty() &&
                    permissions.size == grantResults.size &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            )
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    private fun onPermissionState(granted: Boolean) {
        if (granted) {
            checkBluetoothState()
        } else {
            finish()
        }
    }

    private fun checkBluetoothState() {
        val enabled = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter?.isEnabled
        if (enabled == false) {
            requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            onBluetoothState(enabled == true)
        }
    }

    private fun onBluetoothState(available: Boolean) {
        if (available) {
            checkLocationServicesState()
        } else {
            finish()
        }
    }

    private fun checkLocationServicesState() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
        locationRequest.interval = 100000
        locationRequest.fastestInterval = 10000L

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(this)

        val result: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
                onLocationServicesState(true)
            } catch (ex: ApiException) {
                when (ex.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        requestEnableLocation.launch(IntentSenderRequest.Builder((ex as ResolvableApiException).resolution).build())
                    }

                    else -> {
                        onLocationServicesState(false)
                    }
                }
            }
        }
    }

    private fun onLocationServicesState(available: Boolean) {
        if (available) {
            Handler(Looper.getMainLooper()).post {
                onPermissionsInitialized()
            }
        } else {
            finish()
        }
    }

    private companion object {
        private const val REQUEST_PERMISSIONS = 0x111
    }
}