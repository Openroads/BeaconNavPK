package pk.edu.dariusz.beaconnavpk.manage

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import pk.edu.dariusz.beaconnavpk.R
import pk.edu.dariusz.beaconnavpk.common.IdentifiableElement
import pk.edu.dariusz.beaconnavpk.common.PrepareTokenAndCallTask
import pk.edu.dariusz.beaconnavpk.manage.EditingFragment.Companion.UPDATED_BEACON_MANAGED
import pk.edu.dariusz.beaconnavpk.manage.PaginationScrollListener.Companion.PAGE_SIZE
import pk.edu.dariusz.beaconnavpk.manage.model.BeaconManaged
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.ProximityApiConnector.Companion.PROXIMITY_BEACON_SCOPE_STRING
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.AttachmentEntry
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.GetBeaconAttachmentListResponse
import pk.edu.dariusz.beaconnavpk.utils.BEARER
import pk.edu.dariusz.beaconnavpk.utils.LOCATION_NAME
import pk.edu.dariusz.beaconnavpk.utils.MESSAGE_TYPE
import pk.edu.dariusz.beaconnavpk.utils.getType
import java.lang.ref.WeakReference


/**
 * A fragment representing a list of localizations.
 * Activities containing this fragment MUST implement the
 * [ManageFragment.OnListFragmentInteractionListener] interface.
 */
class ManageFragment : Fragment(), IdentifiableElement {

    private val TAG = "ManageFragment"

    private lateinit var mLayoutManager: LinearLayoutManager
    private val PAGE_START = 1
    private var currentPage = PAGE_START
    private var isLastPage = false
    private var isLoading = false
    private var nextPageToken: String = ""
    private var totalCount: Int = 1
    private val managedBeaconList = mutableListOf<BeaconManaged>()
    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null
    private lateinit var beaconItemRecyclerViewAdapter: BeaconItemRecyclerViewAdapter
    private val proximityApiConnector by lazy {
        ProximityApiConnector.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_list, container, false)

        beaconItemRecyclerViewAdapter = BeaconItemRecyclerViewAdapter(managedBeaconList, listener)
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

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)

        val searchViewItem = menu?.findItem(R.id.app_bar_search)

        searchViewItem?.let { searchVI ->
            searchVI.isVisible = true
            val searchView = searchVI.actionView as SearchView
            val activity = requireActivity()
            val searchManager = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager

            searchView.setSearchableInfo(
                searchManager
                    .getSearchableInfo(activity.componentName)
            )

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return onQueryTextChange(query)
                }

                override fun onQueryTextChange(query: String): Boolean {
                    if (!isLoading) {
                        beaconItemRecyclerViewAdapter.filter.filter(query)
                    }
                    return false
                }
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val appCompatActivity = activity as AppCompatActivity
        appCompatActivity.supportActionBar?.title = "Configuration"
        if (managedBeaconList.isNullOrEmpty()) {
            beaconItemRecyclerViewAdapter.addLoading()
            isLoading = true
            fetchBeacons(null)
        }
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
                    if (pageToken == null) {
                        totalCount = beaconListResponse.totalCount.toInt()
                    }
                    nextPageToken = beaconListResponse.nextPageToken
                    if (currentPage != PAGE_START) {
                        beaconItemRecyclerViewAdapter.removeLoading()
                    }
                    beaconListResponse.beacons
                }
                .flatMap { beacon ->
                    proximityApiConnector.getAttachmentList(BEARER + token, beacon.beaconName)
                        .subscribeOn(Schedulers.io())
                        .filter { t: GetBeaconAttachmentListResponse ->
                            if (t.attachments.isNullOrEmpty() || t.attachments.none { attachmentEntry ->
                                    LOCATION_NAME == getType(attachmentEntry.namespacedType)
                                }) {
                                Log.e(
                                    TAG,
                                    "Beacon: ${beacon.beaconName} not configured. Missing location name in attachments"
                                )
                                totalCount--
                                return@filter false
                            }
                            true
                        }
                        .map { attachmentListResponse ->
                            var locationNameAttachment: AttachmentEntry? = null
                            var messageAttachment: AttachmentEntry? = null

                            for (attachmentEntry in attachmentListResponse.attachments!!) {
                                val type = getType(attachmentEntry.namespacedType)
                                when (type) {
                                    LOCATION_NAME -> locationNameAttachment = attachmentEntry
                                    MESSAGE_TYPE -> messageAttachment = attachmentEntry
                                }
                            }
                            BeaconManaged(beacon.beaconName, locationNameAttachment!!, messageAttachment)
                        }
                }
                .toList()
                .subscribe(
                    { beaconManagedList ->
                        if (currentPage == PAGE_START) {
                            beaconItemRecyclerViewAdapter.removeLoading()
                        }
                        beaconItemRecyclerViewAdapter.addAll(beaconManagedList)
                        if (beaconItemRecyclerViewAdapter.itemCount < totalCount) {
                            beaconItemRecyclerViewAdapter.addLoading()
                        } else {
                            isLastPage = true
                        }
                        isLoading = false
                    },
                    {
                        isLoading = false
                        if (currentPage == PAGE_START) {
                            beaconItemRecyclerViewAdapter.removeLoading()
                        }
                        Log.e(TAG, "Error while fetching beacons..", it)
                    }
                )
        }.execute(PROXIMITY_BEACON_SCOPE_STRING)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == EDIT_MANAGED_BEACON_RC) {
            // if fragment hasn't been recreated again this is not needed because passing objects to fragment
            // don't need marshall object to byte[] and reference to object is send (no IPC process) read more:
            // https://medium.com/@hamidgh/sending-objects-to-fragment-naive-question-is-it-sent-by-value-ddaaa19fa42d)
            // modification in EditingFragment affect directly for object on displayed list
            val beaconToRefresh = data?.getParcelableExtra<BeaconManaged>(UPDATED_BEACON_MANAGED)
            if (beaconToRefresh != null) {
                managedBeaconList.removeAll { b -> b.beaconName == beaconToRefresh.beaconName }
                managedBeaconList.add(beaconToRefresh)
            }
        }
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


    override fun getIdentifier(): String {
        return TAG
    }

    interface OnListFragmentInteractionListener {
        fun onBeaconManageListFragmentInteraction(selectedItem: BeaconManaged?)
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

        const val EDIT_MANAGED_BEACON_RC = 112

        @JvmStatic
        fun newInstance(columnCount: Int) =
            ManageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
