package ch.elca.rovl.functioncomponent.client;

import java.io.IOException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.camel.Exchange;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.functioncomponent.util.ExchangeSerializer;
import io.jsonwebtoken.Jwts;

public class HttpSendClient implements SendClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSendClient.class);

    final CloseableHttpClient httpClient;
    final String functionName;
    final String targetUrl;

    public HttpSendClient(String functionName) {
        this.httpClient = HttpClients.createDefault();
        this.functionName = functionName;
        this.targetUrl = System.getenv("FUNCTION_URL_" + functionName);
        if (targetUrl == null)
            throw new IllegalStateException(String.format(
                "Could not load URL of target function '%s'", functionName));
    }

    @Override
    public void send(Exchange exchange) {
        String jsonedExchange;

        try {
            jsonedExchange = ExchangeSerializer.serialize(exchange);
        } catch (IOException e) {
            LOG.error("Failed to create JSON for Exchange! Dropping message", e);
            return;
        }

        String secretKeyStr = System.getenv("JWT_KEY_" + functionName);
        if (secretKeyStr == null) {
            LOG.error("Missing JWT key for communication with function '" + functionName + "'");
            throw new IllegalStateException("Missing JWT key.");
        }

        byte[] decodedKey = Base64.getDecoder().decode(secretKeyStr);
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
        String jws = Jwts.builder().subject(functionName).signWith(key).compact();

        HttpPost post = new HttpPost(targetUrl);
        post.setEntity(new StringEntity(jsonedExchange));
        post.setHeader("authorization", "Bearer " + jws);

        PostResult result;
        try {
            result = httpClient.execute(post, response -> {
                return new PostResult(response.getCode(), response.getReasonPhrase());
            });
        } catch (IOException exc) {
            LOG.info(String.format(
                "HTTP post error: %s", exc.getMessage()));
            throw new RuntimeException(exc.getMessage());
        }

        if (result.statusCode / 100 == 4 || result.statusCode / 100 == 5) {
            LOG.info(String.format(
                "HTTP post error: [%d] %s", result.statusCode, result.reasonPhrase));
            throw new RuntimeException("HTTP post error.");
        } else {
            LOG.info("HTTP post successful with code " + result.statusCode);
        }
    }

    @Override
    public void stop() {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOG.warn("Failed to close http client at shutdown!");
            e.printStackTrace();
        }
    }

    class PostResult {
        final int statusCode;
        final String reasonPhrase;

        PostResult(int statusCode, String reasonPhrase) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
        }
    }
    
}
