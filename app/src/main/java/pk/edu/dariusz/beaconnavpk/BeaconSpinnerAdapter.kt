package pk.edu.dariusz.beaconnavpk

import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.utils.LOCATION_NAME
import pk.edu.dariusz.beaconnavpk.utils.base64Decode
import pk.edu.dariusz.beaconnavpk.utils.getType

class BeaconSpinnerAdapter(
    private val navigationActivity: NavigationActivity, @LayoutRes resource: Int,
    private val objects: MutableList<BeaconInfo>
) :
    ArrayAdapter<BeaconInfo>(navigationActivity, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val labelText = super.getView(position, convertView, parent) as TextView

        objects[position].attachments.find { attachment ->
            getType(attachment.namespacedType) == LOCATION_NAME
        }?.data?.also {
            val s = base64Decode(it) + " (${objects[position].distance.toString().substring(0, 5)}m)"
            labelText.text = s
        }

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
                navigationActivity.localizationPointsMap[navigationActivity.theNearestLocationMarker] = position
                navigationActivity.drawLocalizationPoints()
            }
        }
        super.notifyDataSetChanged()
    }
}