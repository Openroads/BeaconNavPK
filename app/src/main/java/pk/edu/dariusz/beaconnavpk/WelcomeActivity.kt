package pk.edu.dariusz.beaconnavpk

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_welcome.*
import org.altbeacon.beacon.BeaconManager

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        start_button.setOnClickListener {

            if (verifyBluetooth()) {
                if (verifyLocation()) {
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun verifyBluetooth(): Boolean {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Bluetooth is not enabled")
                dialog.setMessage("Please, enable bluetooth in settings and click start button again.")
                dialog.setPositiveButton(android.R.string.ok, null)
                dialog.show()
                return false
            }
        } catch (e: RuntimeException) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Bluetooth LE not available")
            dialog.setMessage("Sorry, this device does not support Bluetooth Low Energy technology.")
            dialog.setPositiveButton(android.R.string.ok, null)
            dialog.show()

            return false
        }
        return true
    }

    private fun verifyLocation(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M  API >= 23 Permission check
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    val dialog = AlertDialog.Builder(this)
                    dialog.setTitle("Location access not granted")
                    dialog.setMessage(
                        "This app requires location access to discover beacons." +
                                " Please grant location access and click start button again."
                    )
                    dialog.setPositiveButton(android.R.string.ok, null)
                    dialog.setOnDismissListener {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_REQUEST_LOCATION_CODE
                        )
                    }
                    dialog.show()
                } else {
                    val dialog = AlertDialog.Builder(this)
                    dialog.setTitle("Location access not granted")
                    dialog.setMessage(
                        "Please provide location access for application via permission access dialog " +
                                "or application settings and click start button again."
                    )
                    dialog.setPositiveButton(android.R.string.ok, null)
                    dialog.setOnDismissListener {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_REQUEST_LOCATION_CODE
                        )
                    }
                    dialog.show()
                }

                return false
            }
            return true
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_LOCATION_CODE -> {
                if ((grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED))) {
                    Log.d(TAG, "coarse location permission granted")
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Warning !")
                    builder.setMessage("Since location access has not been granted, this app will not be able to work correctly.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.show()
                }
                return
            }
        }
    }

    private val PERMISSION_REQUEST_LOCATION_CODE = 1
    private val TAG = "WelcomeActivity_TAG"
}
