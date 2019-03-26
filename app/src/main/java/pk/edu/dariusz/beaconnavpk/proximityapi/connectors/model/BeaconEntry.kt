package pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model

import pk.edu.dariusz.beaconnavpk.proximityapi.model.AdvertisedId

data class BeaconEntry(val beaconName: String, val advertisedId: AdvertisedId, val description: String)
