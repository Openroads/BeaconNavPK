package pk.edu.dariusz.beaconnavpk.utils

import android.util.Base64


fun base64DecodeToByteArray(s: String): ByteArray {
    return Base64.decode(s, Base64.DEFAULT)
}

/**
 * Decode from base64 encoded String and return as new String
 */
fun base64Decode(s: String): String {
    return String(Base64.decode(s, Base64.NO_WRAP))
}

/**
 * Encode String to base64 and return as new String
 */
fun base64Encode(b: ByteArray): String {
    return Base64.encodeToString(b, Base64.NO_WRAP).trim()
}