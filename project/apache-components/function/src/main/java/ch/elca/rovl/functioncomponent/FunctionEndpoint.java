package ch.elca.rovl.functioncomponent;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultEndpoint;
import ch.elca.rovl.functioncomponent.util.PropertyResolver;
import ch.elca.rovl.functioncomponent.util.TargetProvider;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

/**
 * Function endpoint used to push events to cloud functions.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "function", title = "Function", syntax = "function:name", category = {
        Category.CLOUD })
public class FunctionEndpoint extends DefaultEndpoint {

    @UriPath
    @Metadata(required = true)
    private String name;

    private FunctionComponent component;
    private String functionName;

    public FunctionEndpoint() {
    }

    public FunctionEndpoint(String uri, FunctionComponent component) {
        super(uri, component);
    }

    public FunctionEndpoint(String uri, String remaining, FunctionComponent component) {
        super(uri, component);
        this.functionName = remaining;
        this.component = component;
    }

    /**
     * Creates a function producer.
     */
    public Producer createProducer() throws Exception {
        // if the name of cthe function for the producer is trigger, then the producer
        // is used locally by the function handler to push the received event to the
        // Camel route
        if (functionName.equals("trigger")) {
            return new DirectFunctionProducer(this);
        } else {
            // get the provider of the platform the function is provisioned on, and return
            // the producer
            String providerProperty = String.format("function.%s.provider", functionName);
            TargetProvider provider = PropertyResolver.getTargetProvider(
                    getCamelContext().getPropertiesComponent().resolveProperty(
                            providerProperty));

            return new FunctionProducer(this, functionName, provider);
        }
    }

    /**
     * Creates a consumer. The consumer is used locally to receive events from the
     * function handler and push them to the Camel route.
     */
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer consumer = new DirectFunctionConsumer(this, processor, functionName);
        configureConsumer(consumer);
        return consumer;
    }

    public DirectFunctionConsumer getConsumer() throws InterruptedException {
        return component.getConsumer(functionName);
    }

    /**
     * Some description of this option, and what it does
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
