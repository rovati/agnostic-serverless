package ch.elca.rovl.functioncomponent.client;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.functioncomponent.util.ExchangeSerializer;

public class HttpSendClient implements SendClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSendClient.class);

    CloseableHttpClient httpClient;
    String targetUrl;

    public HttpSendClient(String functionName) {
        this.httpClient = HttpClients.createDefault();
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


        HttpPost post = new HttpPost(targetUrl);
        post.setEntity(new StringEntity(jsonedExchange));

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