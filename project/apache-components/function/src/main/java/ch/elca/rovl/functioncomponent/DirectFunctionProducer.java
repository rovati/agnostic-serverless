package ch.elca.rovl.functioncomponent;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

public class DirectFunctionProducer extends DefaultProducer {

    FunctionComponent component;
    FunctionEndpoint endpoint;
    DirectFunctionConsumer consumer;
    String functionName;
    
    public DirectFunctionProducer(FunctionEndpoint endpoint, String functionName) {
        super(endpoint);
        this.endpoint = endpoint;
        this.component = (FunctionComponent) endpoint.getComponent();
        this.functionName = functionName;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        consumer = endpoint.getConsumer();
        if (consumer == null) {
            throw new IllegalStateException("Couldn't get the consumer.");
        }

        consumer.getProcessor().process(exchange);
    }
    
}
