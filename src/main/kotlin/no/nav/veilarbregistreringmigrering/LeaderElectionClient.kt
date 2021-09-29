package no.nav.veilarbregistreringmigrering

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.boot.json.GsonJsonParser
import java.net.InetAddress
import org.springframework.stereotype.Component

@Component
class LeaderElectionClient {

    private fun getJSONFromUrl(url: String): String {
        val request: Request = Request.Builder()
            .url("http://${url}")
            .header("accept", "application/json")
            .build()

        val restClient = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
        restClient.newCall(request).execute().use {
            if (!it.isSuccessful) {
                throw RuntimeException(
                    "Henting av leader election med statuskode: " + it.code()
                        .toString() + " - " + it
                )

            }
           return it.body()?.string() ?: throw IllegalStateException("Respons fra $url skulle hatt en body ")
        }

    }

    fun isLeader(): Boolean {
        val electorPath = System.getenv("ELECTOR_PATH")
        val leaderJson = GsonJsonParser().parseMap(getJSONFromUrl(electorPath))
        val leader = leaderJson["name"]
        val hostname: String = InetAddress.getLocalHost().hostName
        log.info("Hostname: $hostname, Leader: $leader")
        return hostname == leader
    }

    companion object {
        private val log = loggerFor<LeaderElectionClient>()
    }
}