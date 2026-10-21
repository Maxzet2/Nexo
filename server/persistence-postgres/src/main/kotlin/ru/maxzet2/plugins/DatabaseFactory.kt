package ru.maxzet2.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
    fun init() {
        val ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://postgres:5432/aiplatform"
            username = System.getenv("POSTGRES_USER") ?: "maxzet2"
            password = System.getenv("POSTGRES_PASSWORD") ?: "kchau"
            maximumPoolSize = 10
        })
        Flyway.configure().dataSource(ds).load().migrate()
        Database.connect(ds)
    }
}