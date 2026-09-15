package com.study.payments.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${gateway.pagarme.url}")
    private String pagarmeBaseUrl;

    @Bean
    public RestClient pagarmeRestClient() {
        log.info("Initializing Pagarme RestClient bean with baseUrl: {}", pagarmeBaseUrl);

        // Configuração explícita de fábrica de requisições com timeouts
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(pagarmeBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}