package pk.edu.dariusz.beaconnavpk.common

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.lang.ref.WeakReference

class PrepareTokenAndCallTask(
    private val context: WeakReference<Context>,
    private val resultConsumer: (String?) -> Unit
) : AsyncTask<String, Unit, String>() {

    private val TAG = "PrepareTokenAndCallTask"

    override fun doInBackground(vararg scopes: String): String? {
        Log.i(TAG, "Preparing credential token for scopes: $scopes")
        val credential = GoogleAccountCredential.usingOAuth2(
            context.get(),
            scopes.toList()
        )
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context.get())
        credential.selectedAccount = lastSignedInAccount?.account

        return credential.token //or val token = GoogleAuthUtil.getToken(this@WelcomeActivity, accountName, SCOPE)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.i(TAG, "Post execute with token: " + result?.subSequence(0, 5) + " start executing result consumer..")
        resultConsumer(result)
    }
}


