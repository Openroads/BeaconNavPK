package pk.edu.dariusz.beaconnavpk

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_nav.*
import pk.edu.dariusz.beaconnavpk.connectors.ProximityApiService
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.Observation
import pk.edu.dariusz.beaconnavpk.model.AdvertisedId
import pk.edu.dariusz.beaconnavpk.utils.base64Decode
import java.text.SimpleDateFormat
import java.util.*


class NavigationActivity : AppCompatActivity() {

    private var disposable: Disposable? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private val proximityApiService by lazy {
        ProximityApiService.create()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

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
}
