package pk.edu.dariusz.beaconnavpk.manage


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_manage_item.view.*
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.manage.ManageFragment.OnListFragmentInteractionListener
import pk.edu.dariusz.beaconnavpk.manage.model.BeaconManaged
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.AttachmentEntry


class BeaconItemRecyclerViewAdapter(
    private val managedBeaconList: MutableList<BeaconManaged>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<BaseViewHolder>(), Filterable {

    private var managedBeaconListFiltered = managedBeaconList
    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_LOADING = 1
    private var isLoaderVisible = false

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as BeaconManaged
            mListener?.onBeaconManageListFragmentInteraction(item)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        return when (viewType) {
            VIEW_TYPE_NORMAL -> ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.fragment_manage_item,
                    parent,
                    false
                )
            )
            VIEW_TYPE_LOADING -> FooterHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.loading_item,
                    parent,
                    false
                )
            )
            else -> throw IllegalStateException("Illegal viewType for view holder: $viewType")
        }
    }

    fun add(response: BeaconManaged) {
        managedBeaconList.add(response)
        if (!managedBeaconListFiltered.contains(response)) {
            managedBeaconListFiltered.add(response)
        }
        notifyItemInserted(managedBeaconListFiltered.size - 1)
    }

    fun addAll(beaconItems: List<BeaconManaged>) {
        for (response in beaconItems) {
            add(response)
        }
    }

    private fun remove(postItems: BeaconManaged?) {
        val position = managedBeaconList.indexOf(postItems)
        if (position > -1) {
            managedBeaconList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addLoading() {
        isLoaderVisible = true
        managedBeaconListFiltered.add(
            BeaconManaged(
                "dummyName", AttachmentEntry("d", "d", "", "d"),
                null
            )
        )
    }

    fun removeLoading() {
        isLoaderVisible = false
        val position = managedBeaconListFiltered.size - 1
        val item = getItem(position)
        if (item != null) {
            managedBeaconListFiltered.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clear() {
        while (itemCount > 0) {
            remove(getItem(0))
        }
    }

    fun getItem(position: Int): BeaconManaged? {
        return managedBeaconListFiltered[position]
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoaderVisible && position == managedBeaconListFiltered.size - 1) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = managedBeaconListFiltered.size

    inner class ViewHolder(private val mView: View) : BaseViewHolder(mView) {

        private val mIdView: TextView = mView.manageLocationName
        private val mContentView: TextView = mView.manageLocationDesc

        init {
            mView.setOnClickListener(mOnClickListener)
        }

        override fun clear() {}
        val MAKS_LENGTH = 300
        override fun onBind(position: Int) {
            super.onBind(position)
            val item = managedBeaconListFiltered[position]
            mView.tag = item
            mIdView.text = item.locationNameAttachment.getDataDecoded()

            val messageAttachment = item.messageAttachment
            if (messageAttachment != null) {
                val dataDecoded = messageAttachment.getDataDecoded()
                val content = if (dataDecoded.length > MAKS_LENGTH) {
                    dataDecoded.substring(0, 300) + "...."
                } else {
                    dataDecoded
                }
                mContentView.text = content
            }
        }
    }

    inner class FooterHolder(itemView: View) : BaseViewHolder(itemView) {
        override fun clear() {}
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = if (constraint.isNullOrEmpty() || isLoaderVisible) {
                        managedBeaconList
                    } else {
                        managedBeaconList.filter { mBeacon ->
                            mBeacon.locationNameAttachment.getDataDecoded().contains(constraint, ignoreCase = true)
                        }.toMutableList()
                    }
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null) {
                    managedBeaconListFiltered = results.values as MutableList<BeaconManaged>
                    notifyDataSetChanged()
                }
            }
        }
    }
}
