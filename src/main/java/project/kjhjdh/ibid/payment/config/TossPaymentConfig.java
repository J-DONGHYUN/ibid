package project.kjhjdh.ibid.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class TossPaymentConfig {

    private final String tossPaymentApiUri;
    private final String tosspaymentsSecretKey;

    public TossPaymentConfig(
            @Value("${toss.api.uri:https://api.tosspayments.com/v1/payments}") String tossPaymentApiUri,
            @Value("${toss.api.secret:test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6}") String tossPaymentSecretKey
    ) {
        this.tossPaymentApiUri = tossPaymentApiUri;
        this.tosspaymentsSecretKey = tossPaymentSecretKey;
    }

    @Bean
    public RestClient TossPaymentClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder
                .baseUrl(tossPaymentApiUri)
                .defaultHeader(HttpHeaders.AUTHORIZATION, getAuthorizations())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String getAuthorizations() {
        byte[] encodedBytes = Base64.getEncoder()
                .encode((tosspaymentsSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedBytes);
    }
}
