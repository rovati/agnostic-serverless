package ch.elca.rovl.databasecomponent;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;

import org.apache.camel.support.DefaultComponent;

/**
 * Camel component for integration of the database building block.
 * <p>
 * Its URI is specified as "database:databaseName"
 */
@org.apache.camel.spi.annotations.Component("database")
public class DatabaseComponent extends DefaultComponent {
    
    public DatabaseComponent() {}

    public DatabaseComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // proxy to jdbc component
        return getCamelContext().getEndpoint("jdbc:" + remaining, parameters);
    }
}
