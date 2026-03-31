package com.infinitybutterfly

import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

//fun main(args: Array<String>) {
//    io.ktor.server.netty.EngineMain.main(args)
//}

fun main() {
    // Render provides a "PORT" environment variable. If it's not there, default to 8080 for local testing.
    val port = System.getenv("PORT")?.toInt() ?: 8080

    // You MUST bind to "0.0.0.0" for Render to route traffic to your app
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    DatabaseFactory.init()
}
