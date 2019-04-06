package pk.edu.dariusz.beaconnavpk.manage

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.common.IdentifiableElement
import pk.edu.dariusz.beaconnavpk.common.PrepareTokenAndCallTask
import pk.edu.dariusz.beaconnavpk.manage.PaginationScrollListener.Companion.PAGE_SIZE
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector.Companion.PROXIMITY_BEACON_SCOPE_STRING
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.BeaconEntry
import pk.edu.dariusz.beaconnavpk.utils.BEARER
import java.lang.ref.WeakReference

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ManageFragment.OnListFragmentInteractionListener] interface.
 */
class ManageFragment : Fragment(), IdentifiableElement {
    private lateinit var mLayoutManager: LinearLayoutManager

    private val TAG = "ManageFragment"

    private val PAGE_START = 1

    private var currentPage = PAGE_START
    private var isLastPage = false
    private var isLoading = false
    private var nextPageToken: String = ""
    private var totalCount: Int = 1

    private val proximityApiConnector by lazy {
        ProximityApiConnector.create()
    }

    private var columnCount = 1

    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun getIdentifier(): String {
        return TAG
    }

    private lateinit var beaconItemRecyclerViewAdapter: BeaconItemRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_list, container, false)

        beaconItemRecyclerViewAdapter = BeaconItemRecyclerViewAdapter(mutableListOf(), listener)
        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> {
                        mLayoutManager = LinearLayoutManager(context)
                        mLayoutManager
                    }
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = beaconItemRecyclerViewAdapter
                setHasFixedSize(true)

                addOnScrollListener(
                    object : PaginationScrollListener(mLayoutManager) {
                        override val isLastPage: Boolean
                            get() = this@ManageFragment.isLastPage
                        override val isLoading: Boolean
                            get() = this@ManageFragment.isLoading

                        override fun loadMoreItems() {
                            this@ManageFragment.isLoading = true
                            currentPage++
                            fetchBeacons(nextPageToken)
                        }
                    })
            }
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fetchBeacons(null)
    }

    private fun fetchBeacons(pageToken: String?) {
        PrepareTokenAndCallTask(WeakReference(requireContext())) { token ->
            proximityApiConnector.getBeaconList(
                BEARER + token,
                "",//status:active
                PAGE_SIZE,
                pageToken
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapIterable { beaconListResponse ->
                    nextPageToken = beaconListResponse.nextPageToken
                    totalCount = beaconListResponse.totalCount.toInt()
                    if (currentPage != PAGE_START) {
                        beaconItemRecyclerViewAdapter.removeLoading()
                    }
                    beaconItemRecyclerViewAdapter.addAll(beaconListResponse.beacons)
                    beaconListResponse.beacons
                }
                .flatMap { beacon ->
                    proximityApiConnector.getAttachmentList(BEARER + token, beacon.beaconName)
                        .subscribeOn(Schedulers.io())
                }
                .subscribe(
                    { response ->
                        response.attachments
                    },
                    {
                        Log.e(TAG, "Error while fetching beacons..", it)
                    },
                    {
                        if (beaconItemRecyclerViewAdapter.itemCount < totalCount) {
                            beaconItemRecyclerViewAdapter.addLoading()
                        } else {
                            isLastPage = true
                        }
                        isLoading = false
                    }
                )


        }.execute(PROXIMITY_BEACON_SCOPE_STRING)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: BeaconEntry?)
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int) =
            ManageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
