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

/**
 * Utility class to serialize and deserialize Exchanges.
 * <p>
 * Serialization is used to fit all Exchange data in the body of queue messages,
 * so that all info in the Exchange can travel through any supported queue
 * independently of the paltform event format standard.
 * <p>
 * NOTE serialization is not the correct term. All it does is translating the
 * Exchange from/to a JSON string.
 */
public class ExchangeSerializer {

    /**
     * Serializes an Exchange. Returns a JSON containing all headers and properties
     * and the body of the Exchange.
     * 
     * @param e exchange
     * @return JSON string containing the serialized Exchange
     * @throws JSONObjectException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public static String serialize(Exchange e) throws JSONObjectException, JsonProcessingException, IOException {
        // get exchange data
        Map<String, Object> headers = e.getIn().getHeaders();
        headers = headers == null ? new HashMap<>() : headers;
        Map<String, Object> properties = e.getProperties();
        properties = properties == null ? new HashMap<>() : properties;
        String body = e.getIn().getBody(String.class);
        body = body == null ? "" : body;

        // create JSON
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

        // return JSON as string
        return composer.end()
                .put("body", body)
                .end()
                .finish();
    }

    /**
     * Deserializes a JSON string into the given Exchange.
     * 
     * @param ex Exchange to be filled with the serialized data
     * @param serialized JSON string of the serialized Exchange
     * @throws JSONObjectException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static void deserializeExchange(Exchange ex, String serialized) throws JSONObjectException, IOException {
        // deserialize JSON
        Map<String, Object> content = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                .beanFrom(HashMap.class, serialized);

        // get data
        String body = (String) content.get("body");
        Map<String, Object> properties = (Map<String, Object>) content.get("properties");
        Map<String, Object> headers = (Map<String, Object>) content.get("headers");

        // fill exchange
        ex.getIn().setBody(body);
        for (String key : properties.keySet()) {
            ex.setProperty(key, properties.get(key));
        }
        for (String key : headers.keySet()) {
            ex.getIn().setHeader(key, headers.get(key));
        }
    }
}
