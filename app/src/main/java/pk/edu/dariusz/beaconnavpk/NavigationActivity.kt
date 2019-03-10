package pk.edu.dariusz.beaconnavpk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_navigation_drawer.*
import kotlinx.android.synthetic.main.content_navigation_drawer.*
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import pk.edu.dariusz.beaconnavpk.model.AttachmentInfo
import pk.edu.dariusz.beaconnavpk.model.BeaconInfo
import pk.edu.dariusz.beaconnavpk.model.Position
import pk.edu.dariusz.beaconnavpk.utils.*

class NavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, BeaconConsumer {

    private lateinit var beaconManager: BeaconManager

    private var trackedBeacons: MutableList<Beacon> = mutableListOf()

    private var message: String? = null

    private lateinit var spinnerNearbyBeaconsAdapter: BeaconSpinnerAdapter

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
        setContentView(R.layout.activity_navigation_drawer)
        beaconManager = BeaconManager.getInstanceForApplication(this)

        AndroidThreeTen.init(this)
//        mapImageView.isZoomable = false
        selectedLocationMarker.color = resources.getColor(android.R.color.holo_red_dark, theme)
        theNearestLocationMarker.color = resources.getColor(android.R.color.holo_green_dark, theme)
        map = BitmapFactory.decodeResource(resources, R.drawable.mieszkanie_plan)

        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)
        )
        connectToNetworkSnackInfo = Snackbar.make(
            toolbar, "Nearby localization detected." +
                    " Please, connect to the internet to show information about them.", Snackbar.LENGTH_LONG
        )
        beaconManager.bind(this)

        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(8000L)

        // BeaconManager.setDebug(true)

        /*if(user != in project) //TODO
        nav_view.menu.findItem(R.id.nav_management_section).isVisible = false*/

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
            val items = getTimetabledAttachments(selectedBeacon)
            when {
                items.isNullOrEmpty() -> Toast.makeText(this, "No timetable available", Toast.LENGTH_LONG).show()

                items.size == 1 -> openTimetable(items[0])

                items.size > 1 -> createChooseWeekDialog(items).show()
            }
        }

        toolbar.title = "Navigation"
        setSupportActionBar(toolbar)

        compassButton.setOnClickListener {
            nearby_beacons_spinner.setSelection(0, true)
        }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            /*if activity has back button
            android.R.id.home -> {
                    finish()
                    true
                }*/
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_navigation -> {
            }
            R.id.nav_info -> {

            }
            /*    R.id.nav_share -> {

                }*/
            R.id.nav_manage -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
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

    private fun clearAndDisableViews() {
        open_schedule_button.isEnabled = false; show_message_button.isEnabled = false
        message = null
        selectedLocationText.text = ""
        mapImageView.setImageBitmap(map)
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

                if (isNetworkAvailable(this)) {
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
                } else {
                    if (!connectToNetworkSnackInfo.view.isShown) {
/*                      to show on top
                        val view = connectToNetworkSnackInfo.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params*/
                        connectToNetworkSnackInfo.show()
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
        const val MIN_DISTANCE = 4.5
    }
}
