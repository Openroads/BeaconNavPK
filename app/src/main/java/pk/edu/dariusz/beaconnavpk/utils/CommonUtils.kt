package pk.edu.dariusz.beaconnavpk.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager

fun isNetworkAvailable(activity: Activity): Boolean {
    val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnectedOrConnecting
}