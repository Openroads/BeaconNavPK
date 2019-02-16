package pk.edu.dariusz.beaconnavpk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_nav.*
import org.altbeacon.beacon.*
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

    private var trackedBeacon: Beacon? = null

    private var message: String? = null

    private var locationName: String? = null

    private var trackedProximityBeacon: BeaconInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

        beaconManager.bind(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )

        show_message_button.setOnClickListener {
            message?.let {
                createMessageDialog(it, locationName).show()
            } ?: run {
                Toast.makeText(this, "No message available", Toast.LENGTH_LONG).show()
            }
        }

        open_schedule_button.setOnClickListener {

            val items = getTimetabledAttachments(trackedProximityBeacon)
            when {
                items.isNullOrEmpty() -> Toast.makeText(this, "No timetable available", Toast.LENGTH_LONG).show()

                items.size == 1 -> openTimetable(items[0])

                items.size > 1 -> {
                    val createChooseWeekDialog = createChooseWeekDialog(items)
                    createChooseWeekDialog.show()
                }
            }
        }
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

    private fun beginSearch(searchString: String) {
        val currentTime = Calendar.getInstance().time

        val timestampMs = dateFormat.format(currentTime)

        val getObservedRequest = GetObservedRequest(
            listOf(
                Observation(
                    AdvertisedId(id = searchString),
                    "",
                    timestampMs
                )
            ), "*/*"
        )
        disposable = proximityApiService.getBeaconInfo(getObservedRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    val proximityBeacons = response.beacons
                    Log.i("size: ", proximityBeacons.size.toString())
                    if (proximityBeacons.isNotEmpty()) {
                        trackedProximityBeacon = proximityBeacons[0]
                        val attachments = trackedProximityBeacon!!.attachments

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
                        Log.i("attachment info : ", base64Decode(attachments[0].data))
                    }
                },
                { error ->
                    Log.e("error", error.message)
                    error.printStackTrace()
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
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
                Log.i(TAG, "I just saw an beacon for the first time!")
            }

            override fun didExitRegion(region: Region) {
                Log.i(TAG, "I no longer see an beacon")
            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: $state")
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
                Log.i(
                    TAG,
                    "First beacon I see is about " + beacons.iterator().next().distance + " meters away."
                )

                val closestBeacon: Beacon? =
                    beacons.filter { beacon -> beacon.distance < 2 }.minBy { beacon -> beacon.distance }
                if (closestBeacon != null) {
                    val id1Bytes = closestBeacon.id1.toByteArray()
                    val id2Bytes = closestBeacon.id2.toByteArray()
                    val encodedId = base64Encode(id1Bytes.plus(id2Bytes))

                    if (trackedBeacon != closestBeacon) {
                        Log.i(TAG, "New tracked beacon: " + closestBeacon.id2.toString())
                        Log.i(TAG, "Beacons size : " + beacons.size)
                        beginSearch(encodedId)
                        trackedBeacon = closestBeacon
                    } else {
                        Log.i(
                            TAG, "The closest beacon with id: " + closestBeacon.id2.toString()
                                    + "I see is about " + closestBeacon.distance + " meters away."
                        )
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
    }


    /*private fun mapBeaconAttachmentsWithWeekTypes(beacon: BeaconInfo?): Map<TimetableWeek, AttachmentInfo> {
         val items = mapOf<TimetableWeek, AttachmentInfo>()

         beacon?.attachments?.forEach { attachment ->
             when (attachment.namespacedType) {
                 TimetableWeek.TIMETABLE_EVEN.name -> items.plus(Pair(TimetableWeek.TIMETABLE_EVEN, attachment))
                 TimetableWeek.TIMETABLE_ODD.name -> items.plus(Pair(TimetableWeek.TIMETABLE_ODD, attachment))
                 TimetableWeek.TIMETABLE.name -> items.plus(Pair(TimetableWeek.TIMETABLE, attachment))
             }
         }

         return items
     }*/

}
