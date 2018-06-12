package com.urban.envbot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = arrayOf("me.ramswaroop.jbot", "com.urban.envbot"))
open class EnvbotApplication

fun main(args: Array<String>) {
    SpringApplication.run(EnvbotApplication::class.java, *args)
}
