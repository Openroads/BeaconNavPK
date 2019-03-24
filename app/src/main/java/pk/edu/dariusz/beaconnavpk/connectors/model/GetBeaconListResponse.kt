package pk.edu.dariusz.beaconnavpk.connectors.model

data class GetBeaconListResponse(val beacons: List<BeaconEntry>, val nextPageToken: String, val totalCount: String)
