package pk.edu.dariusz.beaconnavpk.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.AccountPicker
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_welcome.*
import org.altbeacon.beacon.BeaconManager
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.common.PrepareTokenAndCallTask
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector.Companion.PROXIMITY_BEACON_SCOPE_STRING
import pk.edu.dariusz.beaconnavpk.utils.*
import retrofit2.HttpException
import java.lang.ref.WeakReference

/**
 * First activity displayed for user in application. Provides possibility to choose google account used in app.
 * Class provides implementation for preparing environment properly for application such as:
 * checks for bluetooth enable,
 * checks for network connection,
 * checks for required permission,
 * provides way to select google account from device to use in app
 */
@Suppress("PrivatePropertyName")
class WelcomeActivity : AppCompatActivity() {
    //private lateinit var accountSharedPref: SharedPreferences
    private val proximityApiConnector by lazy {
        ProximityApiConnector.create()
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        start_button.setOnClickListener {

            if (verifyBluetooth() and verifyLocation()) {
                if (checkNetworkAndShowInfo(this)) {
                    val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
                    if (!GoogleSignIn.hasPermissions(lastSignedInAccount, SCOPE_PROXIMITY_BEACON, SCOPE_EMAIL)) {
                        GoogleSignIn.requestPermissions(
                            this, AUTHORIZE_BEACON_EDIT_RC, lastSignedInAccount, SCOPE_PROXIMITY_BEACON, SCOPE_EMAIL
                        )
                    } else {
                        designatePermission()
                    }
                }
            }
        }

        sign_in_button.setOnClickListener {
            if (checkNetworkAndShowInfo(this)) {
                startActivityForResult(googleSignInClient.signInIntent, SIGN_IN_GOOGLE_RC)
            }
        }

        sign_out_button.setOnClickListener {
            signOutUser()
        }

        //activity call be called from other when sign out clicked
        val signingOut = intent.extras?.getBoolean(SIGNING_OUT_KEY)
        if (signingOut != null && signingOut) {
            signOutUser()
        }


        /*//TODO old way to choose account
        accountSharedPref = getSharedPreferences(PREFERENCE_ACCOUNT, Context.MODE_PRIVATE)
         val accountName = accountSharedPref.getString(AccountManager.KEY_ACCOUNT_NAME, "")
         if (accountName.isNotBlank()) {
             accountNameTextView.text = accountName
         } else {
             chooseUserAccount()
         }
         accountNameTextView.setOnClickListener { chooseUserAccount() }*/
    }

    private fun signOutUser() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            if (it.isComplete && it.isSuccessful) {
                Toast.makeText(this, "Successfully signed-out.", Toast.LENGTH_SHORT).show()
                updateUIForAccount(null)
            }
        }
    }

    private fun checkNetworkAndShowInfo(activity: Activity): Boolean {
        return if (isNetworkAvailable(activity)) {
            true
        } else {
            Snackbar.make(
                start_button,
                "Application require internet connection. Please connect to the internet.",
                Snackbar.LENGTH_LONG
            ).show()
            false
        }
    }

    override fun onStart() {
        super.onStart()
        updateUIForAccount(GoogleSignIn.getLastSignedInAccount(this))
    }

    private fun startNavigationActivity(canEdit: Boolean) {
        progressBar.visibility = View.GONE
        val intent = Intent(this, NavigationMainActivity::class.java)
        intent.putExtra(IS_EDITOR_KEY, canEdit)
        startActivity(intent)
    }

    private fun updateUIForAccount(account: GoogleSignInAccount?) {
        if (account != null) {
            sign_in_button.visibility = View.GONE
            sign_out_button.visibility = View.VISIBLE
            accountNameTextView.text = account.email
        } else {
            sign_in_button.visibility = View.VISIBLE
            sign_out_button.visibility = View.GONE
            accountNameTextView.text = resources.getString(R.string.empty_account_welcome_activity)
        }
    }

    private fun designatePermission() {
        progressBar.visibility = View.VISIBLE
        try {
            PrepareTokenAndCallTask(WeakReference(this)) { token ->
                proximityApiConnector.getBeaconList(BEARER + token, pageSize = 1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            startNavigationActivity(true)
                        },
                        { error ->
                            if (error is HttpException) {
                                if (error.code() == ACCESS_FORBIDDEN) {
                                    Log.i(TAG, "User don't have permission to edit beacon data " + error.message())
                                    startNavigationActivity(false)
                                } else {
                                    Log.e(TAG, "Error (HttpException) while connecting to proximity api..", error)
                                }
                            } else {
                                Log.e(TAG, "Error while connecting to proximity api..", error)
                            }
                        },
                        {
                            progressBar.visibility = View.GONE
                        }
                    )
            }.execute(PROXIMITY_BEACON_SCOPE_STRING)
        } catch (userRecoverableException: UserRecoverableAuthException) {
            Log.e(TAG, "UserRecoverableAuthException while obtaining credential token", userRecoverableException)
            Toast.makeText(
                this,
                "Missing permission: " + userRecoverableException.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
            startActivityForResult(userRecoverableException.intent, REAUTHORIZE_RC)
        } catch (exception: Exception) {
            Log.e(TAG, "Exception from prepare token task ", exception)
            progressBar.visibility = View.GONE
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /* if (requestCode == CHOOSE_ACCOUNT_RC) {
             if (resultCode == Activity.RESULT_OK) {
                 val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                 accountNameTextView.text = name
                 val editor = accountSharedPref.edit()
                 editor.putString(AccountManager.KEY_ACCOUNT_NAME, name)
                 editor.apply()
             } else if (resultCode == Activity.RESULT_CANCELED) {
                 val string = accountSharedPref.getString(AccountManager.KEY_ACCOUNT_NAME, "")
                 if (string.isNullOrBlank()) {
                     Toast.makeText(this, "Please choose google account to continue.", Toast.LENGTH_SHORT).show()
                 }
             }
         }*/

        if (requestCode == SIGN_IN_GOOGLE_RC) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }

        if (requestCode == AUTHORIZE_BEACON_EDIT_RC || requestCode == REAUTHORIZE_RC) {
            if (resultCode == Activity.RESULT_OK) {
                designatePermission()
            } else {
                Toast.makeText(
                    this,
                    "Please choose google account and accept required permissions to continue.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Toast.makeText(this, "Successfully signed-in.", Toast.LENGTH_SHORT).show()
            updateUIForAccount(account)
        } catch (a: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + a.statusCode)
            Toast.makeText(this, "Sign-in failed. Try again.", Toast.LENGTH_SHORT).show()
            updateUIForAccount(null)
        }
    }

    private fun chooseUserAccount() {
        val accountTypes = arrayOf("com.google")
        val intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, true, null, null, null, null)
        startActivityForResult(intent, CHOOSE_ACCOUNT_RC)
    }

    private val PERMISSION_REQUEST_LOCATION_CODE = 1
    private val TAG = "WelcomeActivity_TAG"
    private val SCOPE = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry"
    private val SCOPE_PROXIMITY_BEACON: Scope = Scope(PROXIMITY_BEACON_SCOPE_STRING)
    private val SCOPE_EMAIL = Scope(Scopes.EMAIL)

    companion object {
        val IS_EDITOR_KEY = "IS_EDITOR"
        val SIGNING_OUT_KEY = "SIGNING_OUT"
    }
}
