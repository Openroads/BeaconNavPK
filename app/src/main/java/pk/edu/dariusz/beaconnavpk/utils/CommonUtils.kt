package pk.edu.dariusz.beaconnavpk.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime

const val CACHE_VALID_TIME_IN_MINUTES = 1

fun isNetworkAvailable(activity: Activity): Boolean {
    val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnectedOrConnecting
}

fun isValidCache(syncTime: LocalDateTime): Boolean {
    val beaconInCacheInMinutes = Duration.between(syncTime, LocalDateTime.now()).toMinutes()

    return beaconInCacheInMinutes < CACHE_VALID_TIME_IN_MINUTES
}

fun isNotValidTracking(syncTime: LocalDateTime): Boolean {
    return !isValidCache(syncTime)
}


/***************** COMMON CONSTANTS ***************/

const val BEARER = "Bearer "

const val PREFERENCE_ACCOUNT = "pk.edu.dariusz.beaconnavpk.account"

const val CHOOSE_ACCOUNT_RC = 21

const val AUTHORIZE_BEACON_EDIT_RC = 22

const val SIGN_IN_GOOGLE_RC = 23

const val REAUTHORIZE_RC = 24

/******************* HTTP ************/

const val ACCESS_FORBIDDEN = 403