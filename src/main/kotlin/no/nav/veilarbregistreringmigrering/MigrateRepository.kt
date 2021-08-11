package no.nav.veilarbregistreringmigrering

import no.nav.veilarbregistreringmigrering.TabellNavn.*
import org.httprpc.sql.Parameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.*
import java.time.ZonedDateTime
import javax.sql.DataSource
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

@Repository
class MigrateRepository(@Autowired val datasource: DataSource) {

//    private fun kobleTilDB(): Connection = DriverManager.getConnection(
//        "jdbc:postgresql://${getRequiredProperty("PAWVEILARBREGISTRERING_HOST")}:${getRequiredProperty("PAWVEILARBREGISTRERING_PORT")}/${
//            getRequiredProperty(
//                "PAWVEILARBREGISTRERING_DATABASE"
//            )
//        }",
//        getRequiredProperty("PAWVEILARBREGISTRERING_USERNAME"),
//        getRequiredProperty("PAWVEILARBREGISTRERING_PASSWORD"),
//    )

    fun hentStoersteId(tabellNavn: TabellNavn): Int {
        val connection = datasource.connection
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(
            "select ${tabellNavn.idKolonneNavn} " +
                    "from ${tabellNavn.name} " +
                    "order by ${tabellNavn.idKolonneNavn} desc limit 1"
        )


        return when (resultSet.next()) {
            true -> {
                val id = resultSet.getInt(tabellNavn.idKolonneNavn)

                /* I dette tilfellet (bruker_profilering) har man 3 rader per id (bruker_registrering_id).
                Må starte fra forrige id dersom vi ikke har et "komplett sett" */
                if (tabellNavn == BRUKER_PROFILERING) {
                    val resultat = connection.createStatement()
                        .executeQuery("select count(*) as $ANTALL from ${tabellNavn.name} where ${tabellNavn.idKolonneNavn} = $id")
                    val raderMedSisteId = if (resultat.next()) resultat.getInt(ANTALL) else 0

                    return if (raderMedSisteId < 3) {
                        println("Fant $raderMedSisteId rader for Id: [${id}]")
                        id - 1
                    } else {
                        println("Fant $raderMedSisteId rader for Id: [${id}]")
                        id
                    }
                }
                id
            }
            false -> 0
        }.also { connection.close() }
    }


    fun settInnRader(tabell: TabellNavn, rader: List<MutableMap<String, Any>>) {
        try {
            if (rader.isEmpty()) return
            // Koble til db
            val connection: Connection = datasource.connection

            // Behandle kolonner vi vet må konverteres
            when (tabell) {
                BRUKER_REGISTRERING, SYKMELDT_REGISTRERING -> {
                    rader.forEach {
                        it["OPPRETTET_DATO"] = ZonedDateTime.parse(it["OPPRETTET_DATO"].toString()).toLocalDateTime()
                    }
                }

                TabellNavn.BRUKER_REAKTIVERING -> {
                    rader.forEach {
                        it["REAKTIVERING_DATO"] =
                            ZonedDateTime.parse(it["REAKTIVERING_DATO"].toString()).toLocalDateTime()
                    }
                }

                TabellNavn.REGISTRERING_TILSTAND -> {
                    rader.forEach {
                        it["OPPRETTET"] = ZonedDateTime.parse(it["OPPRETTET"].toString()).toLocalDateTime()
                        if (it["SIST_ENDRET"] != null) it["SIST_ENDRET"] =
                            ZonedDateTime.parse(it["SIST_ENDRET"].toString()).toLocalDateTime()
                    }
                }

                TabellNavn.OPPGAVE -> {
                    rader.forEach {
                        it["OPPRETTET"] = ZonedDateTime.parse(it["OPPRETTET"].toString()).toLocalDateTime()
                    }
                }

                BRUKER_PROFILERING, MANUELL_REGISTRERING -> {
                }
            }

            // Bygg opp en (Java Persistence API) SQL string for den gitte tabellen
            val jpaSQL =
                if (tabell == BRUKER_PROFILERING)
                    """
            INSERT INTO bruker_profilering (${rader[0].keys.joinToString(postfix = "", prefix = "", separator = ",")}) 
            VALUES(${rader[0].keys.joinToString(prefix = ":", postfix = "", separator = ", :")})
            ON CONFLICT (bruker_registrering_id, profilering_type)
            DO NOTHING
            """
                else
                    """
            INSERT INTO ${tabell.name} (${rader[0].keys.joinToString(postfix = "", prefix = "", separator = ",")}) 
            VALUES(${rader[0].keys.joinToString(prefix = ":", postfix = "", separator = ", :")})
            """

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

    fun hentSjekkerForTabell(tabellNavn: TabellNavn): List<Map<String, Any>> {
        val sql = when (tabellNavn) {
            BRUKER_PROFILERING -> profileringSjekkSql
            BRUKER_REGISTRERING -> brukerRegistreringSjekkSql
            SYKMELDT_REGISTRERING -> sykmeldtRegistreringSjekkSql
            MANUELL_REGISTRERING -> manuellRegistreringSjekkSql
            OPPGAVE -> oppgaveSjekkSql
            BRUKER_REAKTIVERING -> brukerReaktiveringSjekkSql
            REGISTRERING_TILSTAND -> registreringstilstandSjekkSql
        }

        return JdbcTemplate(datasource).queryForList(sql)
    }

    companion object {
        private const val ANTALL = "antall"

        private const val brukerReaktiveringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct aktor_id) as unike_aktor_id
        from registrering_tilstand
        """

        private const val registreringstilstandSjekkSql = """
        select count(*) as antall_rader,
        count(distinct bruker_registrering_id) as unike_brukerregistrering_id
        from registrering_tilstand
        """
        private const val profileringSjekkSql = """
        select count(*) as antall_rader, count(distinct verdi) as unike_verdier, count(distinct profilering_type) as unike_typer 
        from bruker_profilering          
        """

        private const val brukerRegistreringSjekkSql = """
        select count(*) as antall_rader, 
        count(distinct foedselsnummer) as unike_foedselsnummer, 
        count(distinct aktor_id) as unike_aktorer, 
        count(distinct jobbhistorikk) as unike_jobbhistorikk, 
        count(distinct yrkespraksis) as unike_yrkespraksis, 
        floor(avg(konsept_id)) as gjsnitt_konsept_id 
        from bruker_registrering
        """

        private const val sykmeldtRegistreringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct fremtidig_situasjon) as unike_fremtidig_situasjon,
        count(distinct aktor_id) as unike_aktorer,
        count(distinct utdanning_bestatt) as unike_utdanning_bestatt,
        count(distinct andre_utfordringer) as unike_andre_utfordringer,
        round(avg(cast(nus_kode as int)), 2) as gjsnitt_nus from sykmeldt_registrering
        """

        private const val manuellRegistreringSjekkSql = """
        select count(*) as antall_rader,
        count(distinct veileder_ident) as unike_veiledere,
        count(distinct veileder_enhet_id) as unike_enheter,
        count(distinct registrering_id) as unike_registreringer, 
        count(distinct bruker_registrering_type) as unike_reg_typer from manuell_registrering
        """

        private const val oppgaveSjekkSql = """
        select count(*) as antall_rader,
        count(distinct aktor_id) as unike_aktorer,
        count(distinct oppgavetype) as unike_oppgavetyper,
        count(distinct ekstern_oppgave_id) as unike_oppgave_id,
        floor(avg(ekstern_oppgave_id)) as gjsnitt_oppgave_id from oppgave
        """
    }
}
