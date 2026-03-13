package com.example.migration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MigrationGauntletApplication

fun main(args: Array<String>) {
    runApplication<MigrationGauntletApplication>(*args)
}
