package com.plicated

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/hello") {
                call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
            }

            get("/helloM") {
                call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
        .start(wait = true)
}
