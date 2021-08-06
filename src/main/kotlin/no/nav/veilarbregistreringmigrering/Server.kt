package no.nav.veilarbregistreringmigrering

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Server(@Autowired val leaderElectionClient: LeaderElectionClient) {
    @Scheduled(fixedRate = 60000)
    fun migrate() {
        // Lese pg-db, finne tabeller og kolonnenavn
        // Autorisasjon (header som leses) - hentes i veilarbregistrering fra Google secret manager
        // Proxy-er

        if (!leaderElectionClient.isLeader()) {
            return
        }


        val migrateClient = MigrateClient()
        TabellNavn.values().forEach {
            val sisteIndex = hentStoersteId(it)
            migrateClient.hentOgSettInnData(it, sisteIndex)
        }
    }
}
