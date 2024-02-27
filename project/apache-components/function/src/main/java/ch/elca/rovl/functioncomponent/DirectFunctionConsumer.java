package ch.elca.rovl.functioncomponent;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

/**
 * Function consumer used locally to receive events from the function handler and
 * push them to the Camel route.
 */
public class DirectFunctionConsumer extends DefaultConsumer {

    private final FunctionComponent component;
    private final String functionName;

    public DirectFunctionConsumer(FunctionEndpoint endpoint, Processor processor, String functionName) {
        super(endpoint, processor);
        this.component = (FunctionComponent) endpoint.getComponent();
        this.functionName = functionName;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // register the consumer in the component so that it is discoverable by local
        // producers
        component.addConsumer(functionName, this);
    }

}
