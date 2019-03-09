package pk.edu.dariusz.beaconnavpk

import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo

class BeaconSpinnerAdapter(
    private val navigationActivity: NavigationActivity, @LayoutRes resource: Int,
    private val objects: MutableList<BeaconInfo>
) :
    ArrayAdapter<BeaconInfo>(navigationActivity, resource, objects) {

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
                val localizationPointsMap = navigationActivity.localizationPointsMap
                if (localizationPointsMap[navigationActivity.theNearestLocationMarker] != position) {
                    localizationPointsMap[navigationActivity.theNearestLocationMarker] = position
                    navigationActivity.drawLocalizationPoints()
                }
            }
        }
        super.notifyDataSetChanged()
    }
}