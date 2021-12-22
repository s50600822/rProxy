package com.example.reactiveProxy


import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import reactor.netty.transport.ProxyProvider.TypeSpec
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit.MILLISECONDS


@Configuration
class Cfg(
    private val webClientBuilder: WebClient.Builder,
    @Value("#{systemProperties['https.proxyHost']}") private val host: String?,
    @Value("#{systemProperties['https.proxyPort']}") private val port: Int?,
    @Value("#{systemProperties['https.nonProxyHosts']}") private val httpsNonProxyHostsPattern: String?,
    @Value("\${webclient.timeout:-1}") private val webclientTimeout: Int,
) {
    @Bean
    fun myHttpClient() = webClientBuilder
            .configureCodecs(responseMapper())
            .clientConnector(ReactorClientHttpConnector(httpClient()))
            .build()

    private fun httpClient() = HttpClient.create()
        .let { if (host != null && port != null) it.proxy(proxyConfig()) else it }
        .configureTimeout()

    private fun HttpClient.configureTimeout(): HttpClient =
        if (webclientTimeout >= 0) {
            option(CONNECT_TIMEOUT_MILLIS, webclientTimeout)
                .doOnConnected {
                    it.addHandlerLast(ReadTimeoutHandler(webclientTimeout.toLong(), MILLISECONDS))
                    it.addHandlerLast(WriteTimeoutHandler(webclientTimeout.toLong(), MILLISECONDS))
                }
        } else this

    private fun responseMapper() =
        ObjectMapper().apply {
            configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(WRITE_DATES_AS_TIMESTAMPS, false)
            configure(ORDER_MAP_ENTRIES_BY_KEYS, true)
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }

    private fun proxyConfig(): (TypeSpec) -> Unit = {
        val builder = it.type(ProxyProvider.Proxy.HTTP)
            .address(InetSocketAddress(host!!, port!!))
        if (!httpsNonProxyHostsPattern.isNullOrEmpty()) {
            builder.nonProxyHosts(httpsNonProxyHostsPattern)
        }
    }

    private fun WebClient.Builder.configureCodecs(objectMapper: ObjectMapper): WebClient.Builder {
        codecs {
            it.defaultCodecs().apply {
                jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, APPLICATION_JSON))
                jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, APPLICATION_JSON))
            }
        }
        return this
    }
}