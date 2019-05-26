package pk.edu.dariusz.beaconnavpk.utils

/**
 * Extract type from [namespacedType] and return as new string
 */
fun getType(namespacedType: String): String {
    return namespacedType.removePrefix("$PROXIMITY_NAMESPACE/")
}

/**
 * Project namespace
 */
const val PROXIMITY_NAMESPACE = "pkbeaconnavigation"

/**
 * ######### Type keys used in proximity api attachments   #########
 */

/**
 * Key for message displayed about location
 */
const val MESSAGE_TYPE = "MESSAGE"

/**
 * Key for location name displayed for location
 */
const val LOCATION_NAME = "LOCATION_NAME"

/**
 * Key for location type used to designate way for displaying data
 */
const val LOCATION_TYPE = "LOCATION_TYPE"

/**
 * Key for location X coordinate on map
 */
const val POSITION_X = "X"

/**
 * Key for location Y coordinate on map
 */
const val POSITION_Y = "Y"

/**
 * Key for floor where location (beacon) is placed (not used yet, for future purpose)
 */
const val FLOOR = "FLOOR"

/**
 * This enum values are also used in proximity api as "Type" key in attachments
 */
enum class TimetableWeek {
    TIMETABLE_EVEN, TIMETABLE_ODD, TIMETABLE
}
