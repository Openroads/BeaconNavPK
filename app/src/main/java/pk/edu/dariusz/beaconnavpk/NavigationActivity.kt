package pk.edu.dariusz.beaconnavpk

import android.os.Bundle
import android.os.RemoteException
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_nav.*
import org.altbeacon.beacon.*
import pk.edu.dariusz.beaconnavpk.connectors.ProximityApiService
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.Observation
import pk.edu.dariusz.beaconnavpk.model.AdvertisedId
import pk.edu.dariusz.beaconnavpk.utils.base64Decode
import java.text.SimpleDateFormat
import java.util.*


class NavigationActivity : AppCompatActivity(), BeaconConsumer {

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(this)

    private var disposable: Disposable? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private val proximityApiService by lazy {
        ProximityApiService.create()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        beaconManager.bind(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )
        show_message_button.setOnClickListener { beginSearch("EBESExQVFhcYGSAgICAgIQ==") }
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
            .subscribe(
                { response ->
                    Log.i("size: ", response.beacons.size.toString())
                    Log.i("attachment info : ", base64Decode(response.beacons[0].attachments[0].data))
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
                val closestBeacon: Beacon? = beacons.minBy { beacon -> beacon.distance }

                Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().distance + " meters away.")
                Log.i(
                    TAG, "The closest beacon with id: " + closestBeacon?.id2.toString()
                            + "I see is about " + closestBeacon?.distance + " meters away."
                )

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
