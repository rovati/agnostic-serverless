package ch.elca.rovl.functioncomponent;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import ch.elca.rovl.functioncomponent.client.SendClient;
import ch.elca.rovl.functioncomponent.util.TargetProvider;

/**
 * Function producer used to send events to cloud functions.
 */
public class FunctionProducer extends DefaultProducer {

    private final SendClient client;

    public FunctionProducer(Endpoint endpoint, String functionName, TargetProvider provider) {
        super(endpoint);
        // get client for the platform the function is provisioned on
        this.client = FunctionClientFactory.createSendClient(functionName, provider);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        client.send(exchange);
    }

    @Override
    public void doStop() {
        client.stop();
    }

}
