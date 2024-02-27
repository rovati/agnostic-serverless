package ch.elca.rovl.functioncomponent;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

/**
 * Function producer used locally by function handlers to push received events to
 * the Camel route.
 */
public class DirectFunctionProducer extends DefaultProducer {

    private final FunctionEndpoint endpoint;
    private DirectFunctionConsumer consumer;

    public DirectFunctionProducer(FunctionEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // get the corresponding consumer and hand the exchange to it
        consumer = endpoint.getConsumer();
        if (consumer == null) {
            throw new IllegalStateException("Couldn't get the consumer.");
        }
        consumer.getProcessor().process(exchange);
    }

}
