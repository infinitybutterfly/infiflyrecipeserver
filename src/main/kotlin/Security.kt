package com.infinitybutterfly

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val dotenv = dotenv { ignoreIfMissing = true }

    // Fallback logic to ensure it ALWAYS finds a secret
    val jwtSecret = dotenv["JWT_SECRET"] ?: System.getenv("JWT_SECRET") ?: throw Exception("Missing JWT_SECRET")

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Storage API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer("http://localhost:8080/")
                    .build()
            )
            validate { credential ->
                // FIXED: Using a safer check for the user_id claim
                val userId = credential.payload.getClaim("user_id").asString()
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    println("🚨 JWT REJECTED: user_id claim is missing from the token!")
                    null
                }
            }
            // THIS IS OUR DEBUG ALARM:
            challenge { defaultScheme, realm ->
                println("🚨 JWT REJECTED: Signature mismatch, expired, or totally missing from header!")
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing token")
            }
        }
    }
}




//package com.infinitybutterfly
//
//import com.auth0.jwt.JWT
//import com.auth0.jwt.algorithms.Algorithm
//import io.github.cdimascio.dotenv.dotenv
//import io.ktor.server.application.*
//import io.ktor.server.auth.*
//import io.ktor.server.auth.jwt.*
//
//fun Application.configureSecurity() {
//    // 1. Load my hidden variables
//    val dotenv = dotenv { ignoreIfMissing = true }
//    val jwtSecret = dotenv["JWT_SECRET"] ?: throw IllegalArgumentException("Missing JWT_SECRET in .env!")
//
//    // 2. Install and Configure Authentication
//    install(Authentication) {
//        jwt("auth-jwt") {
//            realm = "Storage API"
//            verifier(
//                JWT.require(Algorithm.HMAC256(jwtSecret))
//                    // In production, this issuer would be my real domain name
//                    .withIssuer("http://127.0.0.1:8080/")
//                    .withIssuer("http://localhost:8080/")
//                    .build()
//            )
//            validate { credential ->
//                // Check if the token has a valid user_id attached to it
//                if (credential.payload.getClaim("user_id").asString() != "") {
//                    JWTPrincipal(credential.payload)
//                } else null
//            }
//        }
//    }
//}
//
////
////import com.auth0.jwt.JWT
////import com.auth0.jwt.algorithms.Algorithm
////import io.ktor.http.*
////import io.ktor.serialization.kotlinx.json.*
////import io.ktor.server.application.*
////import io.ktor.server.auth.*
////import io.ktor.server.auth.jwt.*
////import io.ktor.server.plugins.calllogging.*
////import io.ktor.server.plugins.contentnegotiation.*
////import io.ktor.server.request.*
////import io.ktor.server.response.*
////import io.ktor.server.routing.*
////import java.sql.Connection
////import java.sql.DriverManager
////import org.slf4j.event.*
////
////fun Application.configureSecurity() {
////    // Please read the jwt property from the config file if you are using EngineMain
////    val jwtAudience = "jwt-audience"
////    val jwtDomain = "https://jwt-provider-domain/"
////    val jwtRealm = "ktor sample app"
////    val jwtSecret = "secret"
////    authentication {
////        jwt {
////            realm = jwtRealm
////            verifier(
////                JWT
////                    .require(Algorithm.HMAC256(jwtSecret))
////                    .withAudience(jwtAudience)
////                    .withIssuer(jwtDomain)
////                    .build()
////            )
////            validate { credential ->
////                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
////            }
////        }
////    }
////}
