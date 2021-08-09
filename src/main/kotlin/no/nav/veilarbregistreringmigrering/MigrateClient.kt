package no.nav.veilarbregistreringmigrering

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component
import java.io.IOException
import java.lang.reflect.Type
import java.lang.System.getenv

@Component
class MigrateClient {
    val VEILARBREGISTRERING_URL = getenv("VEILARBREGISTRERING_URL")

    fun hentNesteBatchFraTabell(tabell: TabellNavn, sisteIndex: Int): List<MutableMap<String, Any>> {
        val request: Request = Request.Builder()
            .url("$VEILARBREGISTRERING_URL/api/migrering?tabellNavn=${tabell.name}&idSisthentet=${sisteIndex}")
            .header("accept", "application/json")
            .header("x_consumerId", "veilarbregistrering")
            .header("x-token", getenv("MIGRATION_TOKEN"))
            .build()

        try {
            val restClient = OkHttpClient.Builder()
                .followRedirects(false)
                .build()
            restClient.newCall(request).execute().use { response ->
                if (response.code() == 404) {
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
                return databaserader
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}