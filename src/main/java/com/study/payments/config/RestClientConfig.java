package com.study.payments.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${gateway.pagarme.url}")
    private String pagarmeBaseUrl;

    /**
     * Registra o RestClient como um Spring Bean gerenciado no container IoC (Singleton).
     * O client nasce pré-configurado com a URL base e os cabeçalhos padrão para JSON.
     */
    @Bean
    public RestClient pagarmeRestClient() {
        log.info("Initializing Pagarme RestClient bean with baseUrl: {}", pagarmeBaseUrl);

        return RestClient.builder()
                .baseUrl(pagarmeBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}