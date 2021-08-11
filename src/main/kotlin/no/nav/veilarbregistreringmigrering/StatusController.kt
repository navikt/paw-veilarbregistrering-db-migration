package no.nav.veilarbregistreringmigrering

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class StatusController( private val migrationStatusService: MigrationStatusService) {

    @GetMapping("/isready")
    fun isReady() = ResponseEntity.ok("OK")

    @GetMapping("/isalive")
    fun isHealthy() = ResponseEntity.ok("OK")

    @GetMapping("/compareDatabases")
    fun compareDatabases(): List<Tabellsjekk> = migrationStatusService.compareDatabaseStatus()
}