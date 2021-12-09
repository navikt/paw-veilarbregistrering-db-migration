package no.nav.veilarbregistreringmigrering

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.nav.veilarbregistreringmigrering.registrering.RegistreringTilstand
import no.nav.veilarbregistreringmigrering.registrering.Status
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
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
                    log.error("Fant ikke tabell")
                }
                if (!response.isSuccessful) {
                    throw RuntimeException(
                        "Henting av rader feilet med statuskode: " + response.code()
                            .toString() + " - " + response
                    )
                }

                response.body()?.let { body ->
                    val databaserader = Gson().fromJson<List<MutableMap<String, Any>>>(body.string())
                    log.info(databaserader.toString())
                    return databaserader
                } ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun hentSjekkerForTabell(tabell: TabellNavn): List<Map<String, Any>> {
        try {
            restClient.newCall(buildRequest("${VEILARBREGISTRERING_URL}/api/migrering/sjekksum/${tabell.name}"))
                .execute().use { response ->
                response.body()?.let { body ->
                    val str = body.string()
                    log.info("${tabell.name}.json: $str")
                    return Gson().fromJson(str)
                } ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun hentAntallPotensieltOppdaterteTilstander(): Int =
        try {
            restClient.newCall(
                buildRequest("$VEILARBREGISTRERING_URL/api/migrering/registrering-tilstand/antall-potensielt-oppdaterte")
            )
                .execute().use { response ->
                    response.body()?.let { body ->
                        val bodyString = body.string()
                        log.info("Antall tilstander: $bodyString")
                        Gson().fromJson<Map<String, Int>>(bodyString)
                    }
                }?.get("antall") ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
        } catch (e: IOException) {
            0
        }

    fun hentOppdaterteRegistreringStatuser(trengerOppdatering: List<RegistreringTilstand>): Map<Status, List<String>> {
        val map = trengerOppdatering.associate { it.id to it.status }

        return try {
            restClient.newCall(
                requestBuilder("$VEILARBREGISTRERING_URL/api/migrering/registrering-tilstand/hent-oppdaterte-statuser")
                    .post(RequestBody.create(MediaType.parse("application/json"), Gson().toJson(map)))
                    .build()
            ).execute().use { response ->
                response.body()?.let { body ->
                    val bodyString = body.string()
                    log.info("Oppdaterte tilstander: $bodyString")
                    Gson().fromJson<Map<Status, List<String>>>(bodyString)
                }
            } ?: throw RuntimeException("Forventet respons med body, men mottok ingenting")
        } catch (e: IOException) {
            log.error("Error while getting updated statuses", e)
            return emptyMap()
        }
    }


    companion object {
        private fun buildRequest(url: String) =
                requestBuilder(url)
                .build()

        private fun requestBuilder(url: String) =
            Request.Builder()
                .url(url)
                .header("accept", "application/json")
                .header("x_consumerId", "veilarbregistrering")
                .header("x-token", getenv("MIGRATION_TOKEN"))

        private val restClient = OkHttpClient.Builder()
            .readTimeout(60L, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, object: TypeToken<T>() {}.type)
        val VEILARBREGISTRERING_URL = getenv("VEILARBREGISTRERING_URL")!!

        private val log = loggerFor<MigrateClient>()
    }
}