package pk.edu.dariusz.beaconnavpk.utils

import android.util.Base64


fun base64DecodeToByteArray(s: String): ByteArray {
    return Base64.decode(s, Base64.DEFAULT)
}

fun base64Decode(s: String): String {
    return String(Base64.decode(s, Base64.DEFAULT))
}

fun base64Encode(b: ByteArray): String {
    return Base64.encodeToString(b, Base64.DEFAULT).trim()
}