package pk.edu.dariusz.beaconnavpk.model

import org.threeten.bp.LocalDateTime


data class BeaconInfo(
    val advertisedId: AdvertisedId, val beaconName: String, val attachments: List<AttachmentInfo>,
    var fetchDate: LocalDateTime = LocalDateTime.now(), var distance: Double, var attachmentData: AttachmentData
) {
    override fun equals(other: Any?): Boolean = other is BeaconInfo && other.advertisedId == this.advertisedId

    override fun hashCode(): Int {
        return advertisedId.hashCode()
    }
}