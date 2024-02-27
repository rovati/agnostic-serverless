package ch.elca.rovl.queuecomponent;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import ch.elca.rovl.queuecomponent.util.PropertyResolver;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Queue endpoint.
 */
@UriEndpoint(firstVersion = "1.0", scheme = "queue", title = "Queue", syntax = "queue:name", category = {
        Category.CLOUD })
public class QueueEndpoint extends DefaultEndpoint {

    @UriPath
    @Metadata(required = true)
    private String name;

    @UriParam(defaultValue = "false", description = "Used by funciton handlers to use a direct-like component")
    boolean useDirectProducer;

    private QueueComponent component;
    private String queueName;

    public QueueEndpoint() {
    }

    public QueueEndpoint(String uri, QueueComponent component) {
        super(uri, component);
    }

    public QueueEndpoint(String uri, String remaining, QueueComponent component) {
        super(uri, component);
        this.queueName = remaining;
        this.component = component;
    }

    /**
     * Creates a queue producer. The producer is used to route processed events to a
     * target queue.
     */
    @Override
    public Producer createProducer() throws Exception {
        if (useDirectProducer) {
            // return a local producer used to push messages to the Camel route.
            return new DirectQueueProducer(this);
        } else {
            // retrieve platform provider of the queue and return producer
            String providerProperty = String.format("queue.%s.provider", queueName);
            TargetProvider provider = PropertyResolver.getTargetProvider(
                    getCamelContext().getPropertiesComponent().resolveProperty(
                            providerProperty));
            return new QueueProducer(this, queueName, provider);
        }
    }

    /**
     * Creates a queue consumer. The consumer works locally, receives events from
     * the function handler and makes them available in the Camle route.
     */
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer consumer = new DirectQueueConsumer(this, processor, queueName);
        configureConsumer(consumer);
        return consumer;
    }

    public DirectQueueConsumer getConsumer() throws InterruptedException {
        return component.getConsumer(queueName);
    }

    public void setUseDirectProducer(boolean b) {
        this.useDirectProducer = b;
    }

    public boolean isUseDirectProducer() {
        return this.useDirectProducer;
    }

    /**
     * Some description of this option, and what it does
     */
    public void setName(String name) {
        this.name = name;
    }

}
