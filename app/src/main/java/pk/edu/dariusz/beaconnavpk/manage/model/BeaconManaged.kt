package pk.edu.dariusz.beaconnavpk.manage.model

import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.AttachmentEntry

data class BeaconManaged(
    val beaconName: String,
    val locationNameAttachment: AttachmentEntry,
    var messageAttachment: AttachmentEntry?
)