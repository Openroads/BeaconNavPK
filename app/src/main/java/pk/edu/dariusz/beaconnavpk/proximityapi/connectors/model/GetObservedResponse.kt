package pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model

import pk.edu.dariusz.beaconnavpk.proximityapi.model.BeaconInfo

data class GetObservedResponse(val beacons: List<BeaconInfo>)