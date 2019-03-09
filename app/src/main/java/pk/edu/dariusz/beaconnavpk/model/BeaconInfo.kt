package pk.edu.dariusz.beaconnavpk.model

import org.threeten.bp.LocalDateTime


data class BeaconInfo(
    val advertisedId: AdvertisedId, val beaconName: String, val attachments: List<AttachmentInfo>,
    var fetchDate: LocalDateTime = LocalDateTime.now(), var distance: Double, var attachmentData: AttachmentData
)