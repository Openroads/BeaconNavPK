package pk.edu.dariusz.beaconnavpk.navigation

import android.content.Context
import android.util.AttributeSet
import android.widget.Spinner

/**
 * Implementation for spinner with detected nearby locations (beacons).
 * Android [Spinner] needs to be overridden due to undesirable default behaviour of setSelection method.
 * Explained in comment above overridden methods.
 */
class NearBeaconsSpinner : Spinner {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setSelection(position: Int, animate: Boolean) {
        val isAlreadySelected = position == selectedItemPosition
        super.setSelection(position, animate)
        if (isAlreadySelected) {
            // Call onItemSelectedListener because spinner don't do this while click is on already selected item
            onItemSelectedListener?.onItemSelected(this, selectedView, position, selectedItemId)
        }
    }

    override fun setSelection(position: Int) {
        val isAlreadySelected = position == selectedItemPosition
        super.setSelection(position)
        if (isAlreadySelected) {
            // Call onItemSelectedListener because spinner don't do this while click is on already selected item
            onItemSelectedListener?.onItemSelected(this, selectedView, position, selectedItemId)
        }
    }
}

