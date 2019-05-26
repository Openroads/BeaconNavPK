package pk.edu.dariusz.beaconnavpk.common

/**
 * Interface to implement by elements that needs to be identified by some unique value
 */
interface IdentifiableElement {

    /**
     * Return unique identifier used to identify element
     */
    fun getIdentifier(): String
}