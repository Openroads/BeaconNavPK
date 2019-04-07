package pk.edu.dariusz.beaconnavpk.manage.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.AttachmentEntry

@Parcelize
data class BeaconManaged(
    val beaconName: String,
    val locationNameAttachment: AttachmentEntry,
    var messageAttachment: AttachmentEntry?
) : Parcelable