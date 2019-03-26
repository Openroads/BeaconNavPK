package pk.edu.dariusz.beaconnavpk.proximityapi.model

/**
 * Class to store data extracted from beacon attachment info list
 */
data class AttachmentData(var locationName: String = "", var message: String? = null, var mapPosition: Position? = null)