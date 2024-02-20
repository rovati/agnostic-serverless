package ch.elca.rovl.queuecomponent.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;

public class ExchangeSerializer {

    public static String serialize(Exchange e) throws JSONObjectException, JsonProcessingException, IOException {
        Map<String, Object> headers = e.getIn().getHeaders();
        headers = headers == null ? new HashMap<>() : headers;
        Map<String, Object> properties = e.getProperties();
        properties = properties == null ? new HashMap<>() : properties;
        String body = e.getIn().getBody(String.class);
        body = body == null ? "" : body;

        ObjectComposer<ObjectComposer<JSONComposer<String>>> composer = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                .composeString()
                .startObject()
                .startObjectField("properties");

        for (String key : properties.keySet()) {
            composer = composer.putObject(key, properties.get(key));
        }

        composer = composer.end()
                .startObjectField("headers");

        for (String key : headers.keySet()) {
            composer = composer.putObject(key, headers.get(key));
        }

        return composer.end()
                .put("body", body)
                .end()
                .finish();
    }

    @SuppressWarnings("unchecked")
    public static void deserializeExchange(Exchange ex, String serialized) throws JSONObjectException, IOException  {
        Map<String, Object> content = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
            .beanFrom(HashMap.class, serialized);

        String body = (String) content.get("body");
        Map<String,Object> properties = (Map<String,Object>) content.get("properties");
        Map<String,Object> headers = (Map<String,Object>) content.get("headers");

        ex.getIn().setBody(body);
        for (String key : properties.keySet()) {
            ex.setProperty(key, properties.get(key));
        }
        for (String key : headers.keySet()) {
            ex.getIn().setHeader(key, headers.get(key));
        }
    }
}
