package ch.elca.rovl.functioncomponent;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DirectFunctionConsumer extends DefaultConsumer {

    FunctionComponent component;
    String functionName;

    public DirectFunctionConsumer(FunctionEndpoint endpoint, Processor processor, String functionName) {
        super(endpoint, processor);
        this.component = (FunctionComponent) endpoint.getComponent();
        this.functionName = functionName;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        component.addConsumer(functionName, this);
    }
    
}
