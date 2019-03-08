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
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.android.synthetic.main.activity_nav.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import pk.edu.dariusz.beaconnavpk.model.AttachmentInfo
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.utils.*


class NavigationActivity : AppCompatActivity(), BeaconConsumer {

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)

    private var trackedBeacons: MutableList<Beacon> = mutableListOf()

    private var message: String? = null

    private lateinit var spinnerNearbyBeaconsAdapter: BeaconSpinnerAdapter

    private val trackedProximityBeacons: MutableList<BeaconInfo> = mutableListOf()

    private lateinit var map: Bitmap

    private val localizationMarker = Paint(ANTI_ALIAS_FLAG)

    init {
        localizationMarker.color = Color.RED
    }

    private lateinit var proximityApiManager: ProximityApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        AndroidThreeTen.init(this)

        map = BitmapFactory.decodeResource(resources, R.drawable.mieszkanie_plan)

        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )

        beaconManager.bind(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Navigation"
        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(8000L)

        // BeaconManager.setDebug(true)

        spinnerNearbyBeaconsAdapter = BeaconSpinnerAdapter(
            this,
            android.R.layout.simple_spinner_item,
            trackedProximityBeacons
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            nearby_beacons_spinner.adapter = adapter
            nearby_beacons_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val itemAtPosition = parent.getItemAtPosition(position) as BeaconInfo
                    updateViewForBeacon(itemAtPosition)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        proximityApiManager =
            ProximityApiManager(this, spinnerNearbyBeaconsAdapter, trackedProximityBeacons)

        show_message_button.setOnClickListener {
            Log.i(TAG, "Show message button onClick")
            message?.let {
                createMessageDialog(it, selectedLocationText.text.toString()).show()
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun drawLocalizationPoint(x: Float, y: Float) {
        /* val drawableBitMap  = imageView.drawable as BitmapDrawable
        val iw = value.intrinsicWidth
        val width = value.bitmap.width*/
        imageView.setImageBitmap(map)

        val tempMutableBitmap = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempMutableBitmap)

        tempCanvas.drawBitmap(map, 0F, 0F, null)

        tempCanvas.drawCircle(x, y, 10f, localizationMarker)

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

    private fun updateViewForBeacon(beaconInfo: BeaconInfo) {
        Log.i(TAG, "Updating views for beacon: $beaconInfo")

        val attachments = beaconInfo.attachments

        clearAndDisableViews()

        var x: Float? = null
        var y: Float? = null

        for (attachment: AttachmentInfo in attachments) {
            if (attachment.namespacedType.contains(TimetableWeek.TIMETABLE.name)) {
                open_schedule_button.isEnabled = true
            }

            val type = getType(attachment.namespacedType)

            when (type) {
                MESSAGE_TYPE -> {
                    show_message_button.isEnabled = true
                    message = base64Decode(attachment.data)
                }
                LOCATION_NAME -> selectedLocationText.text = base64Decode(attachment.data)
                POSITION_X -> x = convertToLocalizationCoordinate(base64Decode(attachment.data))
                POSITION_Y -> y = convertToLocalizationCoordinate(base64Decode(attachment.data))
            }

        }
        imageView.setImageBitmap(map)
        if (x != null && y != null) {
            drawLocalizationPoint(x, y)
        }
    }

    fun clearAndDisableViews() {
        open_schedule_button.isEnabled = false; show_message_button.isEnabled = false
        message = null

        imageView.setImageBitmap(map)
    }


    private fun convertToLocalizationCoordinate(stringCoordinate: String): Float {
        return stringCoordinate.replace(',', '.').toFloat()
    }

    override fun onPause() {
        super.onPause()
        proximityApiManager.dispose()
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

                partition.second.forEach { farBeacon ->
                    proximityApiManager.removeFromTrackedProximityBeacons(
                        encodeBeaconId(farBeacon)
                    )
                }
                trackedBeacons.removeAll(partition.second)

                val closestBeacons = partition.first
                /*.minBy { beacon -> beacon.distance }*/
                if (!closestBeacons.isNullOrEmpty()) {
                    for (closestBeacon in closestBeacons) {
                        Log.i(TAG, "Close beacon with distance less than $MIN_DISTANCE is: $closestBeacon ")
                        if (!trackedBeacons.contains(closestBeacon)) {
                            Log.i(TAG, "That beacon is new tracked beacon..")

                            try {
                                proximityApiManager.addToTrackedProximityBeacons(closestBeacon)
                                trackedBeacons.add(closestBeacon)

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
        const val MIN_DISTANCE = 3.5
    }
}
