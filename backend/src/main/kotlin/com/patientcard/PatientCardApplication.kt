package com.patientcard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PatientCardApplication

fun main(args: Array<String>) {
    runApplication<PatientCardApplication>(*args)
}
