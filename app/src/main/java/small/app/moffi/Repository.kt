package small.app.moffi

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


const val BASE_URL = "https://api.moffi.io/api/"

data class User(
    @JsonProperty("email") val email: String,
    @JsonProperty("password") val password: String,
    @JsonProperty("captcha") val captcha: String = "NOT_PROVIDED"
) {
    fun isNotBlank(): Boolean = email.isNotBlank() && password.isNotBlank()
}

interface MoffiApi {

    @Headers("Content-Type: application/json")
    @POST("signin")
    fun signing(@Body userData: User): Call<JsonNode>

    @Headers("Content-Type: application/json")
    @GET("users/buildings")
    fun buildings(@Header("Authorization") token: String): Call<JsonNode>

    @Headers("Content-Type: application/json")
    @GET("buildings/{id}")
    fun buildingDetails(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Call<JsonNode>

    @Headers("Content-Type: application/json")
    @GET("workspaces/availabilities")
    fun workspaceDetails(
        @Header("Authorization") token: String,
        @QueryMap(encoded = true) params: Map<String, String>
    ): Call<JsonNode>

    @Headers("Content-Type: application/json")
    @POST("orders/add")
    fun order(@Header("Authorization") token: String, @Body order: Order): Call<JsonNode>

}

const val PREF_COOKIES = "PREF_COOKIES"

class AddCookiesInterceptor(private val context: Context) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val builder = chain.request().newBuilder()
        for (cookie in PreferenceManager.getDefaultSharedPreferences(context)
            .getStringSet(PREF_COOKIES, HashSet())!!) {
            builder.addHeader("Cookie", cookie)
            Log.v(
                "OkHttp",
                "Adding Header: $cookie"
            ) // This is done so I know which headers are being added; this interceptor is used after the normal logging of OkHttp
        }
        return chain.proceed(builder.build())
    }
}

class ReceivedCookiesInterceptor(private val context: Context) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalResponse = chain.proceed(chain.request())
        if (originalResponse.headers("Set-Cookie").isNotEmpty()) {
            val cookies = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet("PREF_COOKIES", HashSet()) as HashSet<String>?
            for (header in originalResponse.headers("Set-Cookie")) {
                cookies!!.add(header)
            }
            val memes = PreferenceManager.getDefaultSharedPreferences(context).edit()
            memes.putStringSet("PREF_COOKIES", cookies).apply()
            memes.apply()
        }
        return originalResponse
    }
}

class ServiceBuilder(context: Context) {
    private val okHttpClient: OkHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(AddCookiesInterceptor(context))
        .addInterceptor(ReceivedCookiesInterceptor(context))
        .build()

    private val retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()

    fun <T> buildService(service: Class<T>): T {
        return retrofit.create(service)
    }
}

class RestApiService(context: Context) {
    private val retrofit = ServiceBuilder(context).buildService(MoffiApi::class.java)

    fun signing(userData: User) = retrofit.signing(userData)
        .execute()
        .body()
        ?.get("token")
        ?.textValue()

    fun getCities(token: String) =
        retrofit.buildings(token = token)
            .execute()
            .body()

    fun getCityDetails(token: String, id: String) =
        retrofit.buildingDetails(token = token, id = id)
            .execute()
            .body()

    fun getWorkspaceDetails(
        token: String, id: String,
        startDate: String = now(),
        endDate: String = now(),
        level: Int
    ) =
        retrofit.workspaceDetails(
            token = token, params = mapOf(
                "buildingId" to id,
                "startDate" to startDate,
                "endDate" to endDate,
                "places" to "1",
                "period" to "DAY",
                "floor" to level.toString()
            )
        )
            .execute()
            .body()

    private fun now(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return current.format(formatter)
    }

    fun order(token: String, order: Order) =
        retrofit.order(token = token, order = order).execute().getBody()

    fun <T> Response<T>.getBody(): Any? =
        when (isSuccessful) {
            true -> {
                Log.d("Repository", "Query ok")
                body()
            }
            false -> {
                Log.d("Repository", "Query ko")
                errorBody()
            }
        }


}

suspend fun <T, R> Iterable<T>?.mapAsync(transform: (T) -> R): List<R>? = this?.map {
    CoroutineScope(Dispatchers.IO).async {
        transform(it)
    }
}?.awaitAll()

class Repository(applicationContext: Context) {
    private val apiService = RestApiService(applicationContext)
    private var token: String = ""
    private var user: User = User("", "")
    suspend fun getData(user: User): List<Building> {
        Log.d("Repository", "Fetch data")
        this.user = user
        token = signIn(user)
        Log.d("Repository", "Token fetch")
        val cities = apiService.getCities("Bearer $token")
            ?.mapAsync {
                Building(
                    id = it["id"].textValue(),
                    name = it["name"].textValue(),
                    companyId = it["company"]["id"].textValue()
                )
            }
        Log.d("Repository", "Cities fetch")

        val citiesDetails = cities?.mapAsync {
            apiService.getCityDetails(token, it.id)?.let { d ->
                Building(
                    id = d["id"].textValue(),
                    name = d["name"].textValue(),
                    companyId = d["company"]["id"].textValue(),
                    floors = d.get("floors")?.let { n ->
                        n.map { f ->
                            Floor(
                                f["name"].textValue(),
                                f["level"].asInt()
                            )
                        }
                    } ?: emptyList())
            } ?: Building("", "", "", emptyList())
        } ?: emptyList()
        Log.d("Repository", "Cities details fetch")
        val buildings = citiesDetails.map { b ->
            CoroutineScope(Dispatchers.IO).async {
                val floors = b.floors.mapAsync { f ->
                    apiService.getWorkspaceDetails(token, b.id, level = f.level)?.let { nodes ->
                        val workspaces = nodes.map { node ->
                            val seats = node["workspace"]["seats"]?.map { seat ->
                                mapper.readValue(seat.toString(), Seat::class.java)
                            } ?: emptyList()
                            Workspace(
                                id = node["id"].asText(),
                                name = node["workspace"]["title"].asText(),
                                companyId = b.companyId,
                                seats = seats
                            )
                        }
                        f.copy(workspace = workspaces)
                    } ?: f
                } ?: emptyList()
                b.copy(floors = floors)
            }
        }.awaitAll()
        Log.d("Repository", "Building built")
        return buildings
    }

    private fun signIn(user: User) =
        apiService.signing(user) ?: throw IllegalStateException("Token not check")

    fun reserve(order: Order) =
        if (user.isNotBlank()) apiService.order(
            token,
            order
        ) else throw IllegalStateException(
            "Not signed in"
        )

}