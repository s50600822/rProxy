package com.example.reactiveProxy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RproxyApplication

fun main(args: Array<String>) {
    runApplication<RproxyApplication>(*args)
}
