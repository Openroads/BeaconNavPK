package pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model

data class AttachmentEntry(
    val attachmentName: String,
    val namespacedType: String,
    val data: String,
    val creationTimeMS: String
)
