package ch.elca.rovl.functioncomponent;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.functioncomponent.client.SendClient;
import ch.elca.rovl.functioncomponent.util.TargetProvider;

public class FunctionProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionProducer.class);

    SendClient client;

    public FunctionProducer(Endpoint endpoint, String functionName, TargetProvider provider) {
        super(endpoint);

        LOG.info("Creating function producer for " + functionName);

        this.client = FunctionClientFactory.createSendClient(functionName, provider);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        LOG.info(String.format("Sending message '%s'", exchange.getIn().getBody(String.class)));
        client.send(exchange);
        LOG.info("Message sent.");
    }

    @Override
    public void doStop() {
        client.stop();
    }
    
}
