package pk.edu.dariusz.beaconnavpk.manage


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_manage_item.view.*
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.manage.ManageFragment.OnListFragmentInteractionListener
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.BeaconEntry
import pk.edu.dariusz.beaconnavpk.proximityapi.model.AdvertisedId

class BeaconItemRecyclerViewAdapter(
    private val mValues: MutableList<BeaconEntry>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<BaseViewHolder>() {

    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_LOADING = 1
    private var isLoaderVisible = false

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as BeaconEntry

            mListener?.onListFragmentInteraction(item)
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

    fun add(response: BeaconEntry) {
        mValues.add(response)
        notifyItemInserted(mValues.size - 1)
    }

    fun addAll(beaconItems: List<BeaconEntry>) {
        for (response in beaconItems) {
            add(response)
        }
    }

    private fun remove(postItems: BeaconEntry?) {
        val position = mValues.indexOf(postItems)
        if (position > -1) {
            mValues.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addLoading() {
        isLoaderVisible = true
        add(BeaconEntry("dummyName", AdvertisedId(id = "d"), "dummyDesc"))
    }

    fun removeLoading() {
        isLoaderVisible = false
        val position = mValues.size - 1
        val item = getItem(position)
        if (item != null) {
            mValues.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clear() {
        while (itemCount > 0) {
            remove(getItem(0))
        }
    }

    fun getItem(position: Int): BeaconEntry? {
        return mValues[position]
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoaderVisible && position == mValues.size - 1) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(private val mView: View) : BaseViewHolder(mView) {

        private val mIdStatusView: TextView = mView.bStatus
        private val mIdView: TextView = mView.bName
        private val mContentView: TextView = mView.bDescription

        init {
            mView.setOnClickListener(mOnClickListener)
        }

        override fun clear() {}

        override fun onBind(position: Int) {
            super.onBind(position)
            val item = mValues[position]
            mView.tag = item
            mIdView.text = item.beaconName
            mContentView.text = item.description
        }
    }

    inner class FooterHolder(itemView: View) : BaseViewHolder(itemView) {
        override fun clear() {}
    }
}
