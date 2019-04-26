package pk.edu.dariusz.beaconnavpk.utils


fun getType(namespacedType: String): String {
    return namespacedType.removePrefix("$PROXIMITY_NAMESPACE/")
}


const val PROXIMITY_NAMESPACE = "beacon-pk"

/**
 * Type keys used in proximity api attachments
 */

const val MESSAGE_TYPE = "MESSAGE"

const val LOCATION_NAME = "LOCATION_NAME"

const val LOCATION_TYPE = "LOCATION_TYPE"

const val POSITION_X = "X"

const val POSITION_Y = "Y"

const val FLOOR = "FLOOR"

/**
 * This enum values are also used in proximity api as "Type" key in attachments
 */
enum class TimetableWeek {
    TIMETABLE_EVEN, TIMETABLE_ODD, TIMETABLE
}
