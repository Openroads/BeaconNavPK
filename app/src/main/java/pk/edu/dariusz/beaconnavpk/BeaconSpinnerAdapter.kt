package pk.edu.dariusz.beaconnavpk

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.utils.LOCATION_NAME
import pk.edu.dariusz.beaconnavpk.utils.base64Decode
import pk.edu.dariusz.beaconnavpk.utils.getType

class BeaconSpinnerAdapter(context: Context, @LayoutRes resource: Int, private val objects: MutableList<BeaconInfo>) :
    ArrayAdapter<BeaconInfo>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val labelText = super.getView(position, convertView, parent) as TextView

        objects[position].attachments.find { attachment ->
            getType(attachment.namespacedType) == LOCATION_NAME
        }?.data?.also {
            labelText.text = base64Decode(it)
        }


        return labelText
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }
}