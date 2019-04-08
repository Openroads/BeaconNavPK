package pk.edu.dariusz.beaconnavpk.proximityapi.connectors

import io.reactivex.Observable
import org.threeten.bp.LocalDateTime
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.*
import pk.edu.dariusz.beaconnavpk.proximityapi.model.AdvertisedId
import pk.edu.dariusz.beaconnavpk.utils.LOCATION_NAME
import retrofit2.Response

class ProximityApiConnectorMock : ProximityApiConnector {

    override fun deleteAttachments(
        authHeader: String,
        beaconName: String,
        namespacedType: String?
    ): Observable<BatchDeleteResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun deleteAttachment(authHeader: String, attachmentName: String): Observable<Response<Void>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun createAttachment(
        authHeader: String,
        beaconName: String,
        attachmentEntry: AttachmentEntry
    ): Observable<AttachmentEntry> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBeaconInfo(request: GetObservedRequest): Observable<GetObservedResponse> {
        return Observable.just(GetObservedResponse(emptyList()))
    }

    override fun getBeaconList(
        authHeader: String,
        filter: String?,
        pageSize: Int?,
        pageToken: String?
    ): Observable<GetBeaconListResponse> {
//        Thread.sleep(600)
        val beacons: MutableList<BeaconEntry> = mutableListOf()
        for (i in 0..10) {
            beacons.add(
                BeaconEntry(
                    "Sala g$i",
                    AdvertisedId(id = "advertisedId$i"),
                    "Proszę zaczekać. Jadę opóźniony i będę za 15 minut. Przepraszam za niedogodności."
                )
            )
        }

        return Observable.just(GetBeaconListResponse(beacons, "dummyToken", beacons.size.toString()))
    }

    override fun getAttachmentList(
        authHeader: String,
        beaconName: String
    ): Observable<GetBeaconAttachmentListResponse> {
//        Thread.sleep(500)
        val attachmentList: MutableList<AttachmentEntry> = mutableListOf()
        for (i in 0..2) {
            attachmentList.add(
                AttachmentEntry(
                    "attachmentName$i", "namespace", "",
                    LocalDateTime.now().toString()
                )
            )
        }
        attachmentList.add(
            AttachmentEntry(
                "attachmentName", "beacon-pk/$LOCATION_NAME", "TGFib3JhdG9yaXVtIDE1MDAxMDkwMCBhc2Rh",
                LocalDateTime.now().toString()
            )
        )
        return Observable.just(GetBeaconAttachmentListResponse(attachmentList))
    }

}