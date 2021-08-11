package no.nav.veilarbregistreringmigrering

import org.springframework.stereotype.Service

@Service
class MigrationStatusService(
    private val migrateClient: MigrateClient,
    private val migrateRepository: MigrateRepository
) {

    fun compareDatabaseStatus(): List<Tabellsjekk> {
        val kilde = TabellNavn.values()
            .associate { it to migrateClient.hentSjekkerForTabell(it) }

        val destinasjon = TabellNavn.values()
            .associate { it to migrateRepository.hentSjekkerForTabell(it)[0] }

        println("Hentet statuser fra veilarbregistrering: $kilde")
        println("Hentet status fra lokal database: $destinasjon")

        return kilde.map { (tabell, resultat) ->

            val kolonnerSomIkkeMatcher: List<String> = resultat.filterNot { (kolonne, verdi) ->
                verdi == destinasjon[tabell]?.get(kolonne) ?: false
            }.keys.toList()

            Tabellsjekk(tabell ,kolonnerSomIkkeMatcher.isEmpty(), kolonnerSomIkkeMatcher)
        }

    }
}

