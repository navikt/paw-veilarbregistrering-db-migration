package no.nav.veilarbregistreringmigrering

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.stereotype.Component

@Component
open class LeaderElectionClient {

    private fun getJSONFromUrl(url: String): JSONObject {
        val request: Request = Request.Builder()
            .url("http://${url}")
            .header("accept", "application/json")
            .build()

        val restClient = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
        restClient.newCall(request).execute().use {
            if (!it.isSuccessful()) {
                throw RuntimeException(
                    "Henting av rader feilet med statuskode: " + it.code()
                        .toString() + " - " + it
                )

            }
           return JSONObject(it?.body()?.string())

        }

    }

    open fun isLeader(): Boolean {
        val electorPath = System.getenv("ELECTOR_PATH")
        val leaderJson: JSONObject = getJSONFromUrl(electorPath)
        val leader = leaderJson.getString("name")
        val hostname: String = InetAddress.getLocalHost().hostName
        println(hostname)
        return hostname == leader
    }
}