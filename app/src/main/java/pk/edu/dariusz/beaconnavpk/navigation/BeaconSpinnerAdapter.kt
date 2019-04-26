package pk.edu.dariusz.beaconnavpk.navigation

import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import pk.edu.dariusz.beaconnavpk.proximityapi.model.BeaconInfo

class BeaconSpinnerAdapter(
    private val navigateFragment: NavigateFragment, @LayoutRes resource: Int,
    private val objects: MutableList<BeaconInfo>
) : ArrayAdapter<BeaconInfo>(navigateFragment.activity, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val labelText = super.getView(position, convertView, parent) as TextView
        labelText.text = ""
        labelText.setPadding(0, 0, 0, 0)
        return labelText
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val text = objects[position].attachmentData.locationName +
                " (${objects[position].distance.toString().substring(0, 4)}m)"
        view.text = text

        return view
    }

    override fun getItem(position: Int): BeaconInfo? {
        return if (objects.isNullOrEmpty()) null else objects[position]
    }

    override fun notifyDataSetChanged() {
        if (objects.isNotEmpty()) {
            objects.sortBy { beaconInfo -> beaconInfo.distance }
            val nearestBeacon = objects[0]
            nearestBeacon.attachmentData.mapPosition?.let { position ->
                val localizationPointsMap = navigateFragment.localizationPointsMap
                if (localizationPointsMap[navigateFragment.theNearestLocationMarker] != position) {
                    localizationPointsMap[navigateFragment.theNearestLocationMarker] = position
                    if (navigateFragment.isVisible) {
                        navigateFragment.drawLocalizationPoints()
                    }
                }
            }
        }
        super.notifyDataSetChanged()
    }
}