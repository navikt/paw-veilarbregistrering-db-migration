package no.nav.veilarbregistreringmigrering

data class Tabellsjekk(val tabellNavn: TabellNavn, val ok: Boolean, val feilendeKolonner: List<String>)