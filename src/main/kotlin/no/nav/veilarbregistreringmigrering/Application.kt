package no.nav.veilarbregistreringmigrering

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

@SpringBootApplication
@EnableScheduling
open class Application

fun main(vararg args: String) {
    // Delay startup until db port can be connected to (wait for cloud sql proxy)
    val dbHost = System.getenv("PAWVEILARBREGISTRERING_HOST")?:throw IllegalStateException("Missing environment variable PAWVEILARBREGISTRERING_HOST")
    val dbPort = Integer.parseInt(System.getenv("PAWVEILARBREGISTRERING_PORT"))

    println("Waiting for database port at ${dbHost}:${dbPort} to become connectable ..")
    waitForPort(dbHost, dbPort)
    runApplication<Application>(*args)
}

private fun waitForPort(host: String, port: Int): Boolean {
    for (attempt in IntRange(1, 10)) {
        try {
            Socket(InetAddress.getByName(host), port).use {
                println("Socket at ${host}:${port} is connectable at attempt ${attempt}")
                return true
            }
        } catch (io: IOException) {
            println("Socket at ${host}:${port} is not ready yet: ${io.message}")
        }
        Thread.sleep(1000)
    }
    return false
}

