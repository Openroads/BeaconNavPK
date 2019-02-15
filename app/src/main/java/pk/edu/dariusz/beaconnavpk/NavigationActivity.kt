package pk.edu.dariusz.beaconnavpk

import android.os.Bundle
import android.os.RemoteException
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
import pk.edu.dariusz.beaconnavpk.utils.base64Decode
import pk.edu.dariusz.beaconnavpk.utils.base64Encode
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        beaconManager.bind(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )
        show_message_button.setOnClickListener { /*beginSearch("EBESExQVFhcYGSAgICAgIQ==") */ }
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
                    Log.i("size: ", response.beacons.size.toString())
                    val attachments = response.beacons[0].attachments

                    open_schedule_button.isEnabled = false
                    show_message_button.isEnabled = false
                    for (attachment: AttachmentInfo in attachments) {
                        if (attachment.namespacedType.contains("TIMETABLE")) {
                            open_schedule_button.isEnabled = true
                        }

                        if (attachment.namespacedType.contains("MESSAGE")) {
                            show_message_button.isEnabled = true
                        }
                    }
                    Log.i("attachment info : ", base64Decode(attachments[0].data))
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
        beaconManager.addRangeNotifier { beacons, region ->
            if (beacons.isNotEmpty()) {
                Log.i(
                    TAG,
                    "First beacon I see is about " + beacons.iterator().next().distance + " meters away."
                )

                val closestBeacon: Beacon? =
                    beacons.filter { beacon -> beacon.distance < 2 }.minBy { beacon -> beacon.distance }
                closestBeacon?.let {
                    //it we are sure that it is not null (useful for class fields)
                    val id1Bytes = it.id1.toByteArray()
                    val id2Bytes = it.id2.toByteArray()
                    val encodedId = base64Encode(id1Bytes.plus(id2Bytes))

                    if (trackedBeacon != it) {
                        Log.i(TAG, "New tracked beacon: " + it.id2.toString())
                        Log.i(TAG, "Beacons size : " + beacons.size)
                        beginSearch(encodedId)
                        trackedBeacon = it
                    } else {
                        Log.i(
                            TAG, "The closest beacon with id: " + it.id2.toString()
                                    + "I see is about " + it.distance + " meters away."
                        )
                    }
                } ?: kotlin.run {
                    // run if closestBeacon is null
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
}
