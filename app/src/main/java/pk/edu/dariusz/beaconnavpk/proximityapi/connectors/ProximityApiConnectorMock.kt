package pk.edu.dariusz.beaconnavpk.proximityapi.connectors

import io.reactivex.Observable
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.*
import pk.edu.dariusz.beaconnavpk.proximityapi.model.AdvertisedId

class ProximityApiConnectorMock : ProximityApiConnector {

    override fun getBeaconInfo(request: GetObservedRequest): Observable<GetObservedResponse> {
        return Observable.just(GetObservedResponse(emptyList()))
    }

    override fun getBeaconList(
        authHeader: String,
        filter: String?,
        pageSize: Int?,
        pageToken: String?
    ): Observable<GetBeaconListResponse> {
        Thread.sleep(600)
        val beacons: MutableList<BeaconEntry> = mutableListOf()
        for (i in 0..10) {
            beacons.add(BeaconEntry("beaconName$i", AdvertisedId(id = "advertisedId$i"), "Blablabla"))
        }

        return Observable.just(GetBeaconListResponse(beacons, "dummyToken", beacons.size.toString()))
    }

    override fun getAttachmentList(
        authHeader: String,
        beaconName: String
    ): Observable<GetBeaconAttachmentListResponse> {
        Thread.sleep(500)
        val attachmentList: MutableList<AttachmentEntry> = mutableListOf()
        for (i in 0..5) {
            attachmentList.add(
                AttachmentEntry(
                    "attachmentName$i", "namespace", "blabla",
                    LocalDateTime.now().toString()
                )
            )
        }

        return Observable.just(GetBeaconAttachmentListResponse(attachmentList))
    }

}