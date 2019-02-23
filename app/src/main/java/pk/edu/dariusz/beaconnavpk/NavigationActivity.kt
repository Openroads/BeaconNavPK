package pk.edu.dariusz.beaconnavpk

import android.content.Intent
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.threetenabp.AndroidThreeTen
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_nav.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.Region
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.connectors.ProximityApiService
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.Observation
import pk.edu.dariusz.beaconnavpk.model.AdvertisedId
import pk.edu.dariusz.beaconnavpk.model.AttachmentInfo
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.utils.*
import java.text.SimpleDateFormat
import java.util.*


class NavigationActivity : AppCompatActivity(), BeaconConsumer {

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)

    private var disposable: Disposable? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private val proximityApiService by lazy {
        ProximityApiService.create()
    }

    private var trackedBeacons: MutableList<Beacon> = mutableListOf()

    private var message: String? = null

    private var locationName: String? = null

    private lateinit var spinnerNearbyBeaconsAdapter: BeaconSpinnerAdapter

    private var trackedProximityBeacons: MutableList<BeaconInfo> = mutableListOf()

    private lateinit var map: Bitmap

    private val localizationMarker = Paint(ANTI_ALIAS_FLAG)

    private val beaconProximityAPICache = mutableMapOf<String, BeaconInfo>()

    init {
        localizationMarker.color = Color.RED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        AndroidThreeTen.init(this)

        map = BitmapFactory.decodeResource(resources, R.drawable.mieszkanie_plan)

        beaconManager.bind(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )

//        BeaconManager.setDebug(true)
        spinnerNearbyBeaconsAdapter = BeaconSpinnerAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,//android.R.layout.simple_spinner_item TODO check on different phone
            trackedProximityBeacons
        ).also { adapter ->
            //            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) TODO check on different phone
            nearby_beacons_spinner.adapter = adapter
        }

        nearby_beacons_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val itemAtPosition = parent.getItemAtPosition(position) as BeaconInfo
                updateViewForBeacon(itemAtPosition)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        show_message_button.setOnClickListener {
            Log.i(TAG, "Show message button onClick")
            message?.let {

                val location = nearby_beacons_spinner.selectedView as TextView
                createMessageDialog(it, location.text.toString()).show()
            } ?: run {
                Toast.makeText(this, "No message available", Toast.LENGTH_LONG).show()
            }
        }

        open_schedule_button.setOnClickListener {
            Log.i(TAG, "Open schedule button onClick")
            val beaconInfo = nearby_beacons_spinner.selectedItem as BeaconInfo
            val items = getTimetabledAttachments(beaconInfo)
            when {
                items.isNullOrEmpty() -> Toast.makeText(this, "No timetable available", Toast.LENGTH_LONG).show()

                items.size == 1 -> openTimetable(items[0])

                items.size > 1 -> createChooseWeekDialog(items).show()
            }
        }

        drawLocalizationPoint(252.5F, 186.5F)
    }

    private fun drawLocalizationPoint(x: Float, y: Float) {
        /* val drawableBitMap  = imageView.drawable as BitmapDrawable
        val iw = value.intrinsicWidth
        val width = value.bitmap.width*/

        val tempMutableBitmap = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempMutableBitmap)

        tempCanvas.drawBitmap(map, 0F, 0F, null)


        tempCanvas.drawCircle(x, y, 10f, localizationMarker)

        //temp dots fot tests
        tempCanvas.drawCircle(0f, 0F, 10f, localizationMarker)
        tempCanvas.drawCircle(505F, 373F, 10f, localizationMarker)

        imageView.setImageDrawable(BitmapDrawable(resources, tempMutableBitmap))
    }

    private fun openTimetable(attachmentInfo: AttachmentInfo) {
        val scheduleEndpoint = base64Decode(attachmentInfo.data)
        Log.i(TAG, "Opening timetable in browser..: $scheduleEndpoint")
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(SCHEDULE_URL + scheduleEndpoint))
        startActivity(browser)
    }

    private fun createChooseWeekDialog(items: List<AttachmentInfo>): AlertDialog {
        return AlertDialog.Builder(this).setTitle(R.string.choose_week_dialog_title)
            .setItems(getChooseWeekDialogItems(items)) { _, which ->
                openTimetable(items[which])
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
    }

    private fun createMessageDialog(message: String, location: String?): AlertDialog {
        var title = "Message about "
        title += location ?: "location"
        return AlertDialog.Builder(this).setTitle(title)
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

    private fun addToTrackedProximityBeacons(advertisedBeaconId: String) {
        Log.i(TAG, "Getting proximity data for advertised id: $advertisedBeaconId")

        val beaconFromCache = beaconProximityAPICache[advertisedBeaconId]
        if (beaconFromCache != null && isValidCache(beaconFromCache)) {
            Log.i(TAG, "Taking beacon info from cache...")
            trackedProximityBeacons.add(beaconFromCache)
            runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }

            //updateViewForBeacon(beaconFromCache)
        } else {
            getProximityInfoFromRESTAPI(advertisedBeaconId)
        }
    }

    private fun removeFromTrackedProximityBeacons(advertisedBeaconId: String) {
        trackedProximityBeacons.remove(trackedProximityBeacons.find { beacon -> beacon.advertisedId.id == advertisedBeaconId })
        runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }

    }

    private fun isValidCache(beaconFromCache: BeaconInfo): Boolean {
        val beaconInCacheInMinutes = Duration.between(beaconFromCache.fetchDate, LocalDateTime.now()).toMinutes()

        return beaconInCacheInMinutes < 1
    }

    private fun updateViewForBeacon(beaconInfo: BeaconInfo) {
        Log.i(TAG, "Updating views for beacon: $beaconInfo")

        val attachments = beaconInfo.attachments

        open_schedule_button.isEnabled = false; show_message_button.isEnabled = false

        message = null; locationName = null

        for (attachment: AttachmentInfo in attachments) {
            if (attachment.namespacedType.contains(TimetableWeek.TIMETABLE.name)) {
                open_schedule_button.isEnabled = true
            }

            val type = getType(attachment.namespacedType)
            if (type == MESSAGE_TYPE) {
                show_message_button.isEnabled = true
                message = base64Decode(attachment.data)
            }

            if (type == LOCATION_NAME) {
                locationName = base64Decode(attachment.data)
            }
        }
    }

    private fun getProximityInfoFromRESTAPI(advertisedBeaconId: String) {
        Log.i(TAG, "Fetching beacon info from Proximity API...")

        val currentTime = Calendar.getInstance().time

        val getObservedRequest = GetObservedRequest(
            listOf(Observation(AdvertisedId(id = advertisedBeaconId), "", dateFormat.format(currentTime))),
            "*/*"
        )

        disposable = proximityApiService.getBeaconInfo(getObservedRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    val proximityBeacons = response.beacons

                    if (proximityBeacons.isNotEmpty()) {

                        val beaconInfo = proximityBeacons[0]

                        Log.i(TAG, "Received beacon from proximity: $beaconInfo")

                        beaconInfo.fetchDate = LocalDateTime.now()

                        beaconProximityAPICache[advertisedBeaconId] = beaconInfo

                        trackedProximityBeacons.add(beaconInfo)
                        spinnerNearbyBeaconsAdapter.notifyDataSetChanged()
//                        updateViewForBeacon(beaconInfo)
                    }
                },
                { error ->
                    Log.e("error", error.message)
                    error.printStackTrace()
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                    throw error
                })
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

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
            if (beacons.isNotEmpty()) {

                beacons.forEach { b -> Log.i(TAG, "Scanned beacon: $b  with distance: " + b.distance) }

                val partition = beacons.partition { beacon -> beacon.distance <= MIN_DISTANCE }

                partition.second.forEach { farBeacon -> removeFromTrackedProximityBeacons(encodeBeaconId(farBeacon)) }
                trackedBeacons.removeAll(partition.second)

                val closestBeacons = partition.first
                /*.minBy { beacon -> beacon.distance }*/
                if (!closestBeacons.isNullOrEmpty()) {
                    for (closestBeacon in closestBeacons) {
                        Log.i(TAG, "Close beacon with distance less than $MIN_DISTANCE is: $closestBeacon ")

                        val encodedId = encodeBeaconId(closestBeacon)
                        if (!trackedBeacons.contains(closestBeacon)) {
                            Log.i(TAG, "That beacon is new tracked beacon..")

                            try {
                                addToTrackedProximityBeacons(encodedId)
                                trackedBeacons.add(closestBeacon)

                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Proximity API synchronization for the closest beacon failure: " + e.localizedMessage
                                )
                            }

                        } else {
                            Log.i(TAG, "Close beacon is already tracked..")
                        }
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

    private val TAG = "NavigationActivity_TAG"

    companion object {
        const val SCHEDULE_URL = "http://aslan.mech.pk.edu.pl/"
        const val MIN_DISTANCE = 2
    }
}
