package pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import pk.edu.dariusz.beaconnavpk.utils.base64Decode

@Parcelize
data class AttachmentEntry(
    val attachmentName: String,
    val namespacedType: String,
    val data: String,
    val creationTimeMs: String
) : Parcelable {

    fun getDataDecoded(): String {
        return base64Decode(data)
    }
}