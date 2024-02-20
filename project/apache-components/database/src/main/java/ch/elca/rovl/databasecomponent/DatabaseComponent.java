package ch.elca.rovl.databasecomponent;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;

import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.apache.camel.spi.annotations.Component("database")
public class DatabaseComponent extends DefaultComponent {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseComponent.class);

    public DatabaseComponent() {}

    public DatabaseComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LOG.info("Creating database endpoint with name " + remaining + " and parameters " + parameters.toString());
        return getCamelContext().getEndpoint("jdbc:" + remaining, parameters);
    }
}
