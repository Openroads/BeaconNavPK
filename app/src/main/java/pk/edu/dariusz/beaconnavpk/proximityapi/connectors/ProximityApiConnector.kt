package pk.edu.dariusz.beaconnavpk.proximityapi.connectors

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.GetBeaconAttachmentListResponse
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.GetBeaconListResponse
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.proximityapi.connectors.model.GetObservedResponse
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface ProximityApiConnector {

    /*********************************************************************************************
     * ENDPOINTS
     ********************************************************************************************/
    @POST("./beaconinfo:getforobserved?key=AIzaSyBuPg0CSCO_tZ07Oke1EbTLSMGZIxEy6fg")
    fun getBeaconInfo(@Body request: GetObservedRequest): Observable<GetObservedResponse>

    @GET("beacons")
    fun getBeaconList(
        @Header("Authorization") authHeader: String,
        @Query("q") filter: String? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("pageToken") pageToken: String? = null
    ): Observable<GetBeaconListResponse>

    @GET("{beaconName}/attachments")
    fun getAttachmentList(
        @Header("Authorization") authHeader: String,
        @Path("beaconName", encoded = true) beaconName: String
    ): Observable<GetBeaconAttachmentListResponse>

    /*********************************************************************************************
     * CONFIGURATION
     ********************************************************************************************/
    companion object {
        val PROXIMITY_BEACON_SCOPE_STRING = "https://www.googleapis.com/auth/userlocation.beacon.registry"


        private const val PROXIMITY_API_ENDPOINT: String = "https://proximitybeacon.googleapis.com/v1beta1/"

        private val MOCK = false

        fun create(): ProximityApiConnector {
            if (MOCK) {
                return ProximityApiConnectorMock()
            }
            val interceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.create()
                )
                .client(client)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .baseUrl(PROXIMITY_API_ENDPOINT)
                .build()

            return retrofit.create(ProximityApiConnector::class.java)
        }
    }


}