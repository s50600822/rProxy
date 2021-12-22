package com.example.reactiveProxy

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Duration.ofSeconds
import javax.annotation.PostConstruct

@Component
class SomeFkingComp(private val myHttpClient: WebClient) {
    private val log = LoggerFactory.getLogger(SomeFkingComp::class.java)
    private val url ="https://dev-81543927.okta.com/.well-known/openid-configuration"

    @PostConstruct
    fun doShit() {
        ping()
    }

    fun ping() {
        Flux.interval(Duration.ofMillis(500))
            .parallel()
            .runOn(Schedulers.boundedElastic())// REQUIRED FOR PARALLEL CALL
            .flatMap { test() }
            .subscribe { log.info("{}", it) }
    }

    fun test(): Flux<OktaResponse> {
        return myHttpClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(OktaResponse::class.java)
            .cache()
            .onErrorContinue { t, v -> log.error("WTF", t) }
            .cache(ofSeconds(10L))
            .also { println("GET from REMOTE.....") }
    }

    data class OktaResponse(
        val authorization_endpoint: String,
        val claims_supported: List<String>,
        val code_challenge_methods_supported: List<String>,
        val end_session_endpoint: String,
        val grant_types_supported: List<String>,
        val introspection_endpoint: String,
        val introspection_endpoint_auth_methods_supported: List<String>,
        val issuer: String,
        val jwks_uri: String,
        val registration_endpoint: String,
        val request_object_signing_alg_values_supported: List<String>,
        val request_parameter_supported: Boolean,
        val response_modes_supported: List<String>,
        val response_types_supported: List<String>,
        val revocation_endpoint: String,
        val revocation_endpoint_auth_methods_supported: List<String>,
        val scopes_supported: List<String>,
        val subject_types_supported: List<String>,
        val token_endpoint: String,
        val token_endpoint_auth_methods_supported: List<String>
    )
}