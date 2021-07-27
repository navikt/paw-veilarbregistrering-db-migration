import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.reflect.Type


const val BASE_URL = "http://localhost:8080/veilarbregistrering/api/migrering"

class MigrateClient {

    fun hentOgSettInnData(tabell: TabellNavn, sisteIndex: Int) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}?tabellNavn=${tabell.name}&idSisthentet=${sisteIndex}")
            .header("accept", "application/json")
            .build()

        try {
            val restClient = OkHttpClient.Builder()
                .followRedirects(false)
                .build()
            restClient.newCall(request).execute().use { response ->
                if (response.code() === 404) {
                    println("Fant ikke tabell")
                }
                if (!response.isSuccessful()) {
                    throw RuntimeException(
                        "Henting av rader feilet med statuskode: " + response.code()
                            .toString() + " - " + response
                    )
                }

                val typeToken: Type = object : TypeToken<List<MutableMap<String, Any>>>() {}.type
                val databaserader: List<MutableMap<String, Any>> =  Gson().fromJson(response.body()?.string(), typeToken)
                println(databaserader)
                settInnRader(tabell, databaserader)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}