package no.nav.veilarbregistreringmigrering

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class Application

fun main(vararg args: String) {
    runApplication<Application>(*args)
}

inline fun <reified T:Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java) ?: throw IllegalStateException("Error creating logger")

