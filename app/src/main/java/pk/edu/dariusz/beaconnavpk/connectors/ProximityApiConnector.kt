package pk.edu.dariusz.beaconnavpk.connectors

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedRequest
import pk.edu.dariusz.beaconnavpk.connectors.model.GetObservedResponse
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


interface ProximityApiConnector {

    /*********************************************************************************************
     * ENDPOINTS
     ********************************************************************************************/
    @POST("./beaconinfo:getforobserved?key=AIzaSyBuPg0CSCO_tZ07Oke1EbTLSMGZIxEy6fg")
    fun getBeaconInfo(@Body request: GetObservedRequest): Observable<GetObservedResponse>


    /*********************************************************************************************
     * CONFIGURATION
     ********************************************************************************************/
    companion object {
        private const val PROXIMITY_API_ENDPOINT: String = "https://proximitybeacon.googleapis.com/v1beta1/"

        fun create(): ProximityApiConnector {

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
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