package no.nav.veilarbregistreringmigrering

import no.nav.veilarbregistreringmigrering.log.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MigrateWorker(@Autowired val leaderElectionClient: LeaderElectionClient, @Autowired val repository: MigrateRepository, @Autowired val migrateClient: MigrateClient) {
    @Scheduled(fixedDelay = 20000)
    fun migrate() {
        // Lese pg-db, finne tabeller og kolonnenavn
        // Autorisasjon (header som leses) - hentes i veilarbregistrering fra Google secret manager
        // Proxy-er

        if (!leaderElectionClient.isLeader()) {
            return
        }

        TabellNavn.values().forEach {
            val sisteIndex = repository.hentStoersteId(it)
            val rader = migrateClient.hentNesteBatchFraTabell(it, sisteIndex)
            repository.settInnRader(it, rader)
        }

        if (migrateClient.hentAntallPotensieltOppdaterteTilstander() != repository.antallRaderSomKanTrengeOppdatering()) {
            finnOppdaterteTilstander()
        }
    }

    fun finnOppdaterteTilstander() {
        val trengerOppdatering = repository.hentRaderSomKanTrengeOppdatering()

        val rader = migrateClient.hentOppdaterteRegistreringStatuser(trengerOppdatering)
        logger.info("Hentet oppdaterte rader:", rader)
    }
}
