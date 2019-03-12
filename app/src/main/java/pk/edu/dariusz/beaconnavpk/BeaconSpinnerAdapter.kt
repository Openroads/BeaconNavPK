package pk.edu.dariusz.beaconnavpk

import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo

class BeaconSpinnerAdapter(
    private val navigateFragment: NavigateFragment, @LayoutRes resource: Int,
    private val objects: MutableList<BeaconInfo>
) :
    ArrayAdapter<BeaconInfo>(navigateFragment.activity, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val labelText = super.getView(position, convertView, parent) as TextView

        val s = objects[position].attachmentData.locationName +
                " (${objects[position].distance.toString().substring(0, 4)}m)"

        labelText.text = s

        labelText.setPadding(0, labelText.paddingTop, labelText.paddingRight, labelText.paddingBottom)
        return labelText
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
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