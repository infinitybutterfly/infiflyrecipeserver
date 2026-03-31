package com.infinitybutterfly

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val dotenv = dotenv { ignoreIfMissing = true }
        val dbUrl = dotenv["DATABASE_URL"] ?: throw IllegalArgumentException("DATABASE_URL missing!")

        // Configure the connection pool
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//            connectionTimeout = 30000
            validate()
        }

        val dataSource = HikariDataSource(config)

        // Connect Exposed to the database
        Database.connect(dataSource)

        // Generate the tables if they don't exist yet
        transaction {
//            SchemaUtils.create(Users, Images) // We will create models next!
//            SchemaUtils.createMissingTablesAndColumns(Users, Collections, Texts, Images)
            SchemaUtils.createMissingTablesAndColumns(Users, Recipes, HelpTickets)
        }
    }
}