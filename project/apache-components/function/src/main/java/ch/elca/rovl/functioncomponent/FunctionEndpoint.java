package ch.elca.rovl.functioncomponent;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.functioncomponent.util.PropertyResolver;
import ch.elca.rovl.functioncomponent.util.TargetProvider;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

/**
 * Function component which does bla bla.
 *
 *
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "function", title = "Function", syntax="function:name",
             category = {Category.CLOUD})
public class FunctionEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionEndpoint.class);

    @UriPath @Metadata(required = true)
    private String name;

    FunctionComponent component;
    String functionName;

    public FunctionEndpoint() {}

    public FunctionEndpoint(String uri, FunctionComponent component) {
        super(uri, component);
    }

    public FunctionEndpoint(String uri, String remaining, FunctionComponent component) {
        super(uri, component);
        this.functionName = remaining;
        this.component = component;
    }

    public Producer createProducer() throws Exception {
        if (functionName.equals("trigger")) {
            LOG.info(String.format("Creating direct producer for function '%s'", functionName));
            return new DirectFunctionProducer(this, functionName);
        } else {
            String providerProperty = String.format("function.%s.provider", functionName);
    
            TargetProvider provider = PropertyResolver.getTargetProvider(
                getCamelContext().getPropertiesComponent().resolveProperty(
                    providerProperty));

            return new FunctionProducer(this, functionName, provider);            
        }
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        LOG.info(String.format("Creating direct consumer for function '%s'", functionName));
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
