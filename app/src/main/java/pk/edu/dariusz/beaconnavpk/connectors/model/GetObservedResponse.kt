package pk.edu.dariusz.beaconnavpk.connectors.model

import pk.edu.dariusz.beaconnavpk.model.BeaconInfo

data class GetObservedResponse(val beacons: List<BeaconInfo>)