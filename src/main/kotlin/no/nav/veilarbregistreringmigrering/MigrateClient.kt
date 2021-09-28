package no.nav.veilarbregistreringmigrering

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component
import java.io.IOException
import java.lang.System.getenv
import java.util.concurrent.TimeUnit

@Component
class MigrateClient {

    fun hentNesteBatchFraTabell(tabell: TabellNavn, sisteIndex: Int): List<MutableMap<String, Any>> {
        val request: Request = buildRequest("${VEILARBREGISTRERING_URL}/api/migrering?tabellNavn=${tabell.name}&idSisthentet=${sisteIndex}")

        try {
            restClient.newCall(request).execute().use { response ->
                if (response.code() == 404) {
                    println("Fant ikke tabell")
                }
                if (!response.isSuccessful) {
                    throw RuntimeException(
                        "Henting av rader feilet med statuskode: " + response.code()
                            .toString() + " - " + response
                    )
                }

                response.body()?.let { body ->
                    val databaserader = Gson().fromJson<List<MutableMap<String, Any>>>(body.string())
                    println(databaserader)
                    return databaserader
                } ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun hentSjekkerForTabell(tabell: TabellNavn): List<Map<String, Any>> {
        try {
            restClient.newCall(buildRequest("${VEILARBREGISTRERING_URL}/api/migrering/sjekksum?tabellNavn=${tabell.name}"))
                .execute().use { response ->
                response.body()?.let { body ->
                    val str = body.string()
                    println("${tabell.name}.json: $str")
                    return Gson().fromJson(str)
                } ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }


    companion object {
        private fun buildRequest(url: String) =
            Request.Builder()
                .url(url)
                .header("accept", "application/json")
                .header("x_consumerId", "veilarbregistrering")
                .header("x-token", getenv("MIGRATION_TOKEN"))
                .build()

        private val restClient = OkHttpClient.Builder()
            .readTimeout(60L, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, object: TypeToken<T>() {}.type)
        val VEILARBREGISTRERING_URL = getenv("VEILARBREGISTRERING_URL")!!
    }
}