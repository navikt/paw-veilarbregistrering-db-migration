package no.nav.veilarbregistreringmigrering

import no.nav.veilarbregistreringmigrering.LeaderElectionClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Server(@Autowired val leaderElectionClient: LeaderElectionClient) {
    @Scheduled(fixedRate = 5000)
    fun migrate() {
        // Lese pg-db, finne tabeller og kolonnenavn
        // Autorisasjon (header som leses) - hentes i veilarbregistrering fra Google secret manager
        // Proxy-er

        if (!leaderElectionClient.isLeader()) {
            return
        }


//        val migrateClient = no.nav.veilarbregistreringmigrering.MigrateClient()
//        no.nav.veilarbregistreringmigrering.TabellNavn.values().forEach {
//            val sisteIndex = no.nav.veilarbregistreringmigrering.hentStoersteId(it)
//            migrateClient.hentOgSettInnData(it, sisteIndex)
//        }
    }
}
