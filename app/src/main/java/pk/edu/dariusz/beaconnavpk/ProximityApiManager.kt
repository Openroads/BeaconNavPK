package pk.edu.dariusz.beaconnavpk

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.altbeacon.beacon.Beacon
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.connectors.ProximityApiConnector
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.Observation
import pk.edu.dariusz.beaconnavpk.model.*
import pk.edu.dariusz.beaconnavpk.utils.*
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

    fun addToTrackedProximityBeacons(beacon: Beacon) {
        val advertisedBeaconId = encodeBeaconId(beacon)
        Log.i(TAG, "Getting proximity data for advertised id: $advertisedBeaconId")

        val beaconFromCache = beaconProximityAPICache[advertisedBeaconId]
        if (beaconFromCache != null && isValidCache(beaconFromCache)) {
            Log.i(TAG, "Taking beacon info from cache...")
            beaconFromCache.distance = beacon.distance
            addIfNotExist(beaconFromCache)
            activity.runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }
        } else {
            if (isNetworkAvailable()) {
                getProximityInfoFromRESTAPI(beacon)
            } else {
                Toast.makeText(activity, "Please connect to the internet", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun removeFromTrackedProximityBeacons(advertisedBeaconId: String) {
        if (proximityBeaconListToSync.isNotEmpty()) {
            proximityBeaconListToSync.remove(proximityBeaconListToSync.find { beacon -> beacon.advertisedId.id == advertisedBeaconId })
            activity.runOnUiThread { spinnerNearbyBeaconsAdapter.notifyDataSetChanged() }
            if (proximityBeaconListToSync.isEmpty()) {
                Log.i(TAG, "Tracked proximity beacons collection is empty")
                // activity.clearAndDisableViews()
            }
        }
    }

    fun updateBeaconDistance(closestBeacon: Beacon) {
        val advertisedBeaconId = encodeBeaconId(closestBeacon)
        Log.i(TAG, "Updating beacon $advertisedBeaconId distance for:  ${closestBeacon.distance}")
        proximityBeaconListToSync.find { beaconInfo -> beaconInfo.advertisedId.id == advertisedBeaconId }
            ?.distance = closestBeacon.distance
        spinnerNearbyBeaconsAdapter.notifyDataSetChanged()
    }

    fun dispose() {
        disposable?.dispose()
    }

    private fun getProximityInfoFromRESTAPI(beacon: Beacon) {
        Log.i(TAG, "Fetching beacon info from Proximity API...")

        val advertisedBeaconId = encodeBeaconId(beacon)

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
                        beaconInfo.distance = beacon.distance
                        beaconInfo.attachmentData = extractAttachments(beaconInfo.attachments)
                        beaconProximityAPICache[advertisedBeaconId] = beaconInfo

                        addIfNotExist(beaconInfo)
                        spinnerNearbyBeaconsAdapter.notifyDataSetChanged()
                    }
                },
                { error ->
                    Log.e("error", error.message)
                    error.printStackTrace()
                    Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
                    throw error
                })
    }

    private fun extractAttachments(attachments: List<AttachmentInfo>): AttachmentData {
        val attachmentData = AttachmentData()
        var x: Float? = null
        var y: Float? = null
        for (attachment: AttachmentInfo in attachments) {

            val type = getType(attachment.namespacedType)

            when (type) {
                MESSAGE_TYPE -> {
                    attachmentData.message = base64Decode(attachment.data)
                }
                LOCATION_NAME -> attachmentData.locationName = base64Decode(attachment.data)
                POSITION_X -> x = convertToLocalizationCoordinate(base64Decode(attachment.data))
                POSITION_Y -> y = convertToLocalizationCoordinate(base64Decode(attachment.data))
            }

        }
        if (x != null && y != null) {
            attachmentData.mapPosition = Position(x, y)
        }

        return attachmentData
    }

    private fun convertToLocalizationCoordinate(stringCoordinate: String): Float {
        return stringCoordinate.replace(',', '.').toFloat()
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnectedOrConnecting
    }

    private val TAG = "ProximityApiManager_TAG"
    private val CACHE_VALID_TIME_IN_MINUTES = 1

}