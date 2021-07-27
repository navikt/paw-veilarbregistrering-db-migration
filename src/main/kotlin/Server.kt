
fun main() {
    // Lese pg-db, finne tabeller og kolonnenavn
    // Autorisasjon (header som leses) - hentes i veilarbregistrering fra Google secret manager
    // Proxy-er

    val migrateClient = MigrateClient()

    while (true) {
        TabellNavn.values().forEach {
            val sisteIndex = hentStoersteId(it)
            migrateClient.hentOgSettInnData(it, sisteIndex)
        }

        Thread.sleep(5000)
    }
}