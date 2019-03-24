package pk.edu.dariusz.beaconnavpk

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import pk.edu.dariusz.beaconnavpk.utils.REAUTHORIZE_RC

class PrepareTokenAndCallTask(
    private val activity: Activity,
    val resultConsumer: (String?) -> Unit
) : AsyncTask<String, Unit, String>() {

    private val TAG = "PrepareTokenAndCallTask"

    override fun doInBackground(vararg scopes: String): String? {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                activity,
                scopes.toList()
            )
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(activity)
            credential.selectedAccount = lastSignedInAccount?.account

            return credential.token //or val token = GoogleAuthUtil.getToken(this@WelcomeActivity, accountName, SCOPE)
        } catch (userRecoverableException: UserRecoverableAuthException) {
            Log.e(TAG, "UserRecoverableAuthException while obtaining credential token", userRecoverableException)
            Toast.makeText(
                activity,
                "Missing permission: " + userRecoverableException.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
            activity.startActivityForResult(userRecoverableException.intent, REAUTHORIZE_RC);
        } /*catch (e: Exception) { //TODO handle this out of task  ??
            e.printStackTrace()
            Log.e(TAG, "Exception while obtaining credential token", e)
        }*/
        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        resultConsumer(result)
    }
}


