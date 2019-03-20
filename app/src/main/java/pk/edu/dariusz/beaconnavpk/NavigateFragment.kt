package pk.edu.dariusz.beaconnavpk

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.android.synthetic.main.fragment_navigate.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.model.AttachmentInfo
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.model.IdentifiableElement
import pk.edu.dariusz.beaconnavpk.model.Position
import pk.edu.dariusz.beaconnavpk.utils.*

class NavigateFragment : Fragment(), BeaconConsumer, IdentifiableElement {
    private val TAG = "NavigateFragment_TAG"

    override fun getIdentifier(): String {
        return TAG
    }

    private var message: String? = null

    private lateinit var spinnerNearbyBeaconsAdapter: BeaconSpinnerAdapter

    private lateinit var beaconManager: BeaconManager

    private var trackedBeacons = mutableMapOf<Beacon, LocalDateTime>()

    private val trackedProximityBeacons: MutableList<BeaconInfo> = mutableListOf()

    private lateinit var map: Bitmap

    private val selectedLocationMarker = Paint(Paint.ANTI_ALIAS_FLAG)
    val theNearestLocationMarker = Paint(Paint.ANTI_ALIAS_FLAG)

    val localizationPointsMap = mutableMapOf<Paint, Position>()

    private lateinit var proximityApiManager: ProximityApiManager

    private var selectedBeacon: BeaconInfo? = null

    private lateinit var connectToNetworkSnackInfo: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidThreeTen.init(requireContext())
        beaconManager = BeaconManager.getInstanceForApplication(requireContext())
        // BeaconManager.setDebug(true)
        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(8000L)

        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )

        beaconManager.bind(this)

        map = BitmapFactory.decodeResource(resources, R.drawable.mieszkanie_plan)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this currentFragment
        return inflater.inflate(R.layout.fragment_navigate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //        mapImageView.isZoomable = false
        selectedLocationMarker.color = resources.getColor(android.R.color.holo_red_dark, requireActivity().theme)
        theNearestLocationMarker.color = resources.getColor(android.R.color.holo_green_dark, requireActivity().theme)

        spinnerNearbyBeaconsAdapter = BeaconSpinnerAdapter(
            this,
            R.layout.spinner_row,
            trackedProximityBeacons
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            nearby_beacons_spinner.adapter = adapter
            nearby_beacons_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    selectedBeacon = parent.getItemAtPosition(position) as BeaconInfo
                    selectedBeacon?.let { updateViewForBeacon(it) }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        proximityApiManager =
            ProximityApiManager(requireActivity(), spinnerNearbyBeaconsAdapter, trackedProximityBeacons)

        show_message_button.setOnClickListener {
            Log.i(TAG, "Show message button onClick")
            message?.let {
                createMessageDialog(it, selectedLocationText.text.toString()).show()
            } ?: run {
                Toast.makeText(activity, "No message available", Toast.LENGTH_LONG).show()
            }
        }

        open_schedule_button.setOnClickListener {
            Log.i(TAG, "Open schedule button onClick")
            val items = getTimetabledAttachments(selectedBeacon)
            when {
                items.isNullOrEmpty() -> Toast.makeText(activity, "No timetable available", Toast.LENGTH_LONG).show()

                items.size == 1 -> openTimetable(items[0])

                items.size > 1 -> createChooseWeekDialog(items).show()
            }
        }

        compassButton.setOnClickListener {
            nearby_beacons_spinner.setSelection(0, true)
        }

        connectToNetworkSnackInfo = Snackbar.make(
            compassButton, "Nearby localization detected." +
                    " Please, connect to the internet to show information about them.", Snackbar.LENGTH_LONG
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        (activity as AppCompatActivity).supportActionBar?.title = "Navigation"
        super.onActivityCreated(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        proximityApiManager.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    private fun updateViewForBeacon(beaconInfo: BeaconInfo) {
        Log.i(TAG, "Updating views for beacon: $beaconInfo")

        val attachments = beaconInfo.attachments

        clearAndDisableViews()

        for (attachment: AttachmentInfo in attachments) {
            if (attachment.namespacedType.contains(TimetableWeek.TIMETABLE.name)) {
                open_schedule_button.isEnabled = true
                break
            }
        }

        val attachmentData = beaconInfo.attachmentData

        selectedLocationText.text = attachmentData.locationName

        attachmentData.message?.let { msg ->
            show_message_button.isEnabled = true
            message = msg
        }
        mapImageView.setImageBitmap(map)

        attachmentData.mapPosition?.let { position ->
            localizationPointsMap[selectedLocationMarker] = position
            drawLocalizationPoints()
        }
    }

    fun drawLocalizationPoints() {
        mapImageView.setImageBitmap(map)

        val tempMutableBitmap = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempMutableBitmap)

        tempCanvas.drawBitmap(map, 0F, 0F, null)

        localizationPointsMap.forEach { (marker, position) ->
            tempCanvas.drawCircle(position.x, position.y, 10f, marker)
        }

        mapImageView.setImageDrawable(BitmapDrawable(resources, tempMutableBitmap))
    }

    private fun clearAndDisableViews() {
        open_schedule_button.isEnabled = false; show_message_button.isEnabled = false
        message = null
        selectedLocationText.text = ""
        mapImageView.setImageBitmap(map)
    }


    private fun openTimetable(attachmentInfo: AttachmentInfo) {
        val scheduleEndpoint = base64Decode(attachmentInfo.data)
        Log.i(TAG, "Opening timetable in browser..: $scheduleEndpoint")
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(SCHEDULE_URL + scheduleEndpoint))
        startActivity(browser)
    }

    private fun createChooseWeekDialog(items: List<AttachmentInfo>): AlertDialog {
        return AlertDialog.Builder(requireActivity()).setTitle(R.string.choose_week_dialog_title)
            .setItems(getChooseWeekDialogItems(items)) { _, which ->
                openTimetable(items[which])
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
    }

    private fun createMessageDialog(message: String, location: String?): AlertDialog {
        var title = "Message about "
        title += location ?: "location"
        return AlertDialog.Builder(requireActivity()).setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .create()
    }

    private fun getTimetabledAttachments(beacon: BeaconInfo?): List<AttachmentInfo>? {

        return beacon?.attachments?.filter {
            val type = getType(it.namespacedType)
            type == TimetableWeek.TIMETABLE_ODD.name
                    || type == TimetableWeek.TIMETABLE_EVEN.name
                    || type == TimetableWeek.TIMETABLE.name
        }?.sortedBy { attachment -> attachment.namespacedType }
    }

    private fun getChooseWeekDialogItems(attachments: List<AttachmentInfo>): Array<String> {
        val items = mutableListOf<String>()

        attachments.forEach { attachment ->
            val type = getType(attachment.namespacedType)
            when (type) {
                TimetableWeek.TIMETABLE_EVEN.name -> items.add(resources.getString(R.string.week_even))
                TimetableWeek.TIMETABLE_ODD.name -> items.add(resources.getString(R.string.week_odd))
                TimetableWeek.TIMETABLE.name -> items.add(resources.getString(R.string.other))
            }
        }
        return items.toTypedArray()
    }

    private fun showMissingConnectionInfo() {
        if (!connectToNetworkSnackInfo.view.isShown) {
/*                      to show on top
                        val view = connectToNetworkSnackInfo.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params*/
            connectToNetworkSnackInfo.show()
        }
    }

    /*******************************     BEACON LIBRARY IMPLEMENTATION   **********************************************/
    override fun onBeaconServiceConnect() {

        beaconManager.removeAllMonitorNotifiers()

        beaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                Log.i(TAG, "I just saw an beacon for the first time! - didEnterRegion")
            }

            override fun didExitRegion(region: Region) {
                Log.i(TAG, "I no longer see an beacon - didExitRegion")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                Log.i(
                    TAG,
                    "I have just switched from seeing/not seeing beacons: $state - didDetermineStateForRegion"
                )
            }
        })

        try {
            beaconManager.startMonitoringBeaconsInRegion(Region("myMonitoringUniqueId", null, null, null))
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            if (activity != null) {
                if (beacons.isNotEmpty()) {

                    beacons.forEach { b -> Log.i(TAG, "Scanned beacon: $b  with distance: " + b.distance) }

                    if (isNetworkAvailable(requireActivity())) {
                        val partition = beacons.partition { beacon -> beacon.distance <= MIN_DISTANCE }

                        partition.second.forEach { farBeacon ->
                            proximityApiManager.removeFromTrackedProximityBeacons(
                                encodeBeaconId(farBeacon)
                            )
                        }
                        trackedBeacons.keys.removeAll(partition.second)

                        val closestBeacons = partition.first

                        if (!closestBeacons.isNullOrEmpty()) {
                            for (closestBeacon in closestBeacons) {
                                Log.i(
                                    TAG,
                                    "Close beacon with distance less than $MIN_DISTANCE} is: $closestBeacon "
                                )
                                if (!trackedBeacons.contains(closestBeacon) || isNotValidCache(trackedBeacons[closestBeacon]!!)) {
                                    Log.i(TAG, "That beacon is new tracked beacon..")

                                    try {
                                        proximityApiManager.addToTrackedProximityBeacons(closestBeacon)
                                        trackedBeacons[closestBeacon] = LocalDateTime.now()

                                    } catch (e: Exception) {
                                        Log.e(
                                            TAG,
                                            "Proximity API synchronization for the closest beacon failure: " + e.localizedMessage
                                        )
                                    }

                                } else {
                                    Log.i(TAG, "Close beacon is already tracked..")
                                    proximityApiManager.updateBeaconDistance(closestBeacon)
                                }
                            }
                        }
                    } else {
                        showMissingConnectionInfo()
                    }
                }
            }
        }

        try {
            beaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }
//    override fun getApplicationContext(): Context {
//        return requireContext()
//    }

    override fun getApplicationContext(): Context {
        return requireActivity().applicationContext
    }

    override fun unbindService(serviceConnection: ServiceConnection) {
        requireActivity().unbindService(serviceConnection)
    }

    override fun bindService(intent: Intent, serviceConnection: ServiceConnection, i: Int): Boolean {
        return requireActivity().bindService(intent, serviceConnection, i)
    }

    companion object {

        const val SCHEDULE_URL = "http://aslan.mech.pk.edu.pl/"
        const val MIN_DISTANCE = 4.5

        @JvmStatic
        fun newInstance() = NavigateFragment()
    }
}
