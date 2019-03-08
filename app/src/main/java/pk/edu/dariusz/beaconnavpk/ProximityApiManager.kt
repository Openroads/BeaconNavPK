package pk.edu.dariusz.beaconnavpk

import android.util.Log
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.Observation
import pk.edu.dariusz.beaconnavpk.model.AdvertisedId
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import java.text.SimpleDateFormat
import java.util.*

class ProximityApiManager(
    private val activity: NavigationActivity,
    private val spinnerNearbyBeaconsAdapter: BeaconSpinnerAdapter,
    private val proximityBeaconListToSync: MutableList<BeaconInfo>
) {
    private var disposable: Disposable? = null

    private val proximityApiConnector by lazy {
        ProximityApiConnector.create()
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private val beaconProximityAPICache = mutableMapOf<String, BeaconInfo>()

    fun addToTrackedProximityBeacons(advertisedBeaconId: String) {
        Log.i(TAG, "Getting proximity data for advertised id: $advertisedBeaconId")

        val beaconFromCache = beaconProximityAPICache[advertisedBeaconId]
        if (beaconFromCache != null && isValidCache(beaconFromCache)) {
            Log.i(TAG, "Taking beacon info from cache...")
            if (addIfNotExist(beaconFromCache)) {
                activity.runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }
            }
        } else {
            getProximityInfoFromRESTAPI(advertisedBeaconId)
        }
    }

    fun removeFromTrackedProximityBeacons(advertisedBeaconId: String) {
        if (proximityBeaconListToSync.isNotEmpty()) {
            proximityBeaconListToSync.remove(proximityBeaconListToSync.find { beacon -> beacon.advertisedId.id == advertisedBeaconId })
            activity.runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }
            if (proximityBeaconListToSync.isEmpty()) {
                Log.i(TAG, "Tracked proximity beacons collection is empty")
                activity.clearAndDisableViews()
            }
        }
    }

    fun dispose() {
        disposable?.dispose()
    }

    private fun getProximityInfoFromRESTAPI(advertisedBeaconId: String) {
        Log.i(TAG, "Fetching beacon info from Proximity API...")

        val currentTime = Calendar.getInstance().time

        val getObservedRequest = GetObservedRequest(
            listOf(Observation(AdvertisedId(id = advertisedBeaconId), "", dateFormat.format(currentTime))),
            "*/*"
        )

        disposable = proximityApiConnector.getBeaconInfo(getObservedRequest)
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

                        if (addIfNotExist(beaconInfo)) {
                            spinnerNearbyBeaconsAdapter.notifyDataSetChanged()
                        }
                    }
                },
                { error ->
                    Log.e("error", error.message)
                    error.printStackTrace()
                    Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
                    throw error
                })
    }

    private fun addIfNotExist(beaconInfo: BeaconInfo): Boolean {
        if (!proximityBeaconListToSync.contains(beaconInfo)) {
            return proximityBeaconListToSync.add(beaconInfo)
        }
        return false
    }

    private fun isValidCache(beaconFromCache: BeaconInfo): Boolean {
        val beaconInCacheInMinutes = Duration.between(beaconFromCache.fetchDate, LocalDateTime.now()).toMinutes()

        return beaconInCacheInMinutes < CACHE_VALID_TIME_IN_MINUTES
    }

    private val TAG = "NavigationActivity_TAG"
    private val CACHE_VALID_TIME_IN_MINUTES = 1
}