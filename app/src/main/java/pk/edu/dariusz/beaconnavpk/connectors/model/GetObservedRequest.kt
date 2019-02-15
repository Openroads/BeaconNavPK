package pk.edu.dariusz.beaconnavpk.connectors.model

import pk.edu.dariusz.beaconnavpk.model.AdvertisedId

data class GetObservedRequest(val observations: List<Observation>, val namespacedTypes: String)

data class Observation(val advertisedId: AdvertisedId, val telemetry: String, val timestampMs: String)