package pk.edu.dariusz.beaconnavpk.utils

import org.altbeacon.beacon.Beacon

/**
 * Utilities for operations related with AltBeacon library.
 * @constructor
 */

/**
 * Encode beacon identifier to base64 for [beacon] and return as String
 */
fun encodeBeaconId(beacon: Beacon): String {
    val id1Bytes = beacon.id1.toByteArray()
    val id2Bytes = beacon.id2.toByteArray()
    return base64Encode(id1Bytes.plus(id2Bytes))
}