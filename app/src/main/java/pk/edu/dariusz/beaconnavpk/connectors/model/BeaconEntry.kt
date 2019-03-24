package pk.edu.dariusz.beaconnavpk.connectors.model

import pk.edu.dariusz.beaconnavpk.model.AdvertisedId

data class BeaconEntry(val beaconName: String, val advertisedId: AdvertisedId, val description: String)
