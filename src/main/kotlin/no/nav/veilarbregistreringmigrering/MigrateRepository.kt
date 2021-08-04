package no.nav.veilarbregistreringmigrering

import org.httprpc.sql.Parameters
import java.sql.*
import java.time.ZonedDateTime
import kotlin.system.exitProcess

enum class TabellNavn(val idKolonneNavn: String) {
    BRUKER_REGISTRERING("BRUKER_REGISTRERING_ID"),
    BRUKER_PROFILERING("BRUKER_REGISTRERING_ID"),
    BRUKER_REAKTIVERING("BRUKER_REAKTIVERING_ID"),
    SYKMELDT_REGISTRERING("SYKMELDT_REGISTRERING_ID"),
    MANUELL_REGISTRERING("MANUELL_REGISTRERING_ID"),
    REGISTRERING_TILSTAND("ID"),
    OPPGAVE("ID"),
}

private fun getRequiredProperty(prop: String): String = System.getenv(prop)


private fun kobleTilDB(): Connection = DriverManager.getConnection(
    "jdbc:postgresql://${getRequiredProperty("PAWVEILARBREGISTRERING_HOST")}:${getRequiredProperty("PAWVEILARBREGISTRERING_PORT")}/${getRequiredProperty("PAWVEILARBREGISTRERING_DATABASE")}",
    getRequiredProperty("PAWVEILARBREGISTRERING_USERNAME"),
    getRequiredProperty("PAWVEILARBREGISTRERING_PASSWORD"),
)

fun hentStoersteId(tabellNavn: TabellNavn): Int {
    val connection = kobleTilDB()
    val statment = connection.createStatement()
    val resultSet = statment.executeQuery(
            "select ${tabellNavn.idKolonneNavn} " +
                "from ${tabellNavn.name} " +
                "order by ${tabellNavn.idKolonneNavn} desc limit 1"
    )
    return when (resultSet.next()) {
        true -> resultSet.getInt(tabellNavn.idKolonneNavn)
        false -> 0
    }.also { connection.close() }
}


fun settInnRader(tabell: TabellNavn, rader: List<MutableMap<String, Any>>) {
    try {
        if (rader.isEmpty()) return
        // Koble til db
        val connection: Connection = kobleTilDB()

        // Behandle kolonner vi vet må konverteres
        when (tabell) {
            TabellNavn.BRUKER_REGISTRERING, TabellNavn.SYKMELDT_REGISTRERING -> {
                rader.forEach {
                    it["OPPRETTET_DATO"] = ZonedDateTime.parse(it["OPPRETTET_DATO"].toString()).toLocalDateTime()
                }
            }

            TabellNavn.BRUKER_REAKTIVERING -> {
                rader.forEach {
                    it["REAKTIVERING_DATO"] = ZonedDateTime.parse(it["REAKTIVERING_DATO"].toString()).toLocalDateTime()
                }
            }

            TabellNavn.REGISTRERING_TILSTAND -> {
                rader.forEach {
                    it["OPPRETTET"] = ZonedDateTime.parse(it["OPPRETTET"].toString()).toLocalDateTime()
                    if (it["SIST_ENDRET"] != null) it["SIST_ENDRET"] = ZonedDateTime.parse(it["SIST_ENDRET"].toString()).toLocalDateTime()
                }
            }

            TabellNavn.OPPGAVE -> {
                rader.forEach { it["OPPRETTET"] = ZonedDateTime.parse(it["OPPRETTET"].toString()).toLocalDateTime() }
            }

            TabellNavn.BRUKER_PROFILERING, TabellNavn.MANUELL_REGISTRERING -> { }
        }

        // Bygg opp en (Java Persistence API) SQL string for den gitte tabellen
        val jpaSQL = "INSERT INTO ${tabell.name} " +
                "(${rader[0].keys.joinToString(postfix = "", prefix = "", separator = ",")}) " +
                "VALUES(${rader[0].keys.joinToString(prefix = ":", postfix = "", separator = ", :")})"

        // Send SQL-strengen inn til et Parameters-objekt fra httprpc (named parameteres support for JDBC)
        val parameters: Parameters = Parameters.parse(jpaSQL)

        // Opprett en preparedstatment
        val stmt = connection.prepareStatement(parameters.sql)

        // Traverser hver rad vi har fått inn, map opp kolonneverdiene og legg inn raden i databasen
        rader.forEach {
            parameters.apply(stmt, it)
            println(stmt)
            stmt.execute()
        }
        connection.close()

    } catch (e: Exception) {
        e.printStackTrace()
        System.err.println(e.javaClass.name + ": " + e.message)
        exitProcess(0)
    }
}