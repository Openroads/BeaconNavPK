package pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model

import pk.edu.dariusz.beaconnavpk.utils.base64Decode

data class AttachmentEntry(
    val attachmentName: String,
    val namespacedType: String,
    val data: String,
    val creationTimeMS: String
) {

    fun getDataDecoded(): String {
        return base64Decode(data)
    }
}