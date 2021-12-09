package no.nav.veilarbregistreringmigrering.registrering

import java.time.LocalDateTime

class RegistreringTilstand(
    val id: Long,
    val brukerRegistreringId: Long,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime?,
    val status: Status
)
