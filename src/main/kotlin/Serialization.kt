package com.infinitybutterfly

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.http.invoke
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager
import org.slf4j.event.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        // This tells Ktor to use Kotlinx to parse incoming JSON
        json(Json {
            ignoreUnknownKeys = true // Ignores extra data in Postman/Android!
            isLenient = true         // Forgives minor formatting mistakes (like quotes around booleans)
            prettyPrint = true
        })
    }




//    routing {
//        get("/json/kotlinx-serialization") {
//            call.respond(mapOf("hello" to "world"))
//        }
//    }
}
