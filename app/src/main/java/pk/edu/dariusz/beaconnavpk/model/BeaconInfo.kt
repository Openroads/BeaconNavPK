package pk.edu.dariusz.beaconnavpk.model

data class BeaconInfo(val advertisedId: AdvertisedId, val beaconName: String, val attachments: List<AttachmentInfo>)