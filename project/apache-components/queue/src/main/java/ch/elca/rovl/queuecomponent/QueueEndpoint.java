package ch.elca.rovl.queuecomponent;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.queuecomponent.util.PropertyResolver;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Queue component which offers a Producer that sends the exchage to a queue ona  target provider.
 */
@UriEndpoint(firstVersion = "1.0", scheme = "queue", title = "Queue", syntax="queue:name",
             category = {Category.CLOUD})
public class QueueEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(QueueEndpoint.class);

    @UriPath @Metadata(required = true)
    private String name;

    @UriParam(defaultValue = "false", description = "Used by triggers to use a direct-like component")
    boolean useDirectProducer;

    QueueComponent component;
    String queueName;

    public QueueEndpoint() {}

    public QueueEndpoint(String uri, QueueComponent component) {
        super(uri, component);
    }

    public QueueEndpoint(String uri, String remaining, QueueComponent component) {
        super(uri, component);
        this.queueName = remaining;
        this.component = component;
    }

    @Override
    public Producer createProducer() throws Exception {
        if (useDirectProducer) {
            LOG.info(String.format("Creating direct producer for queue '%s'", queueName));
            return new DirectQueueProducer(this, queueName);
        } else {
            LOG.info("Creating producer for queue " + queueName);
            String providerProperty = String.format("queue.%s.provider", queueName);
            
            TargetProvider provider = PropertyResolver.getTargetProvider(
                getCamelContext().getPropertiesComponent().resolveProperty(
                    providerProperty));
    
            return new QueueProducer(this, queueName, provider);
        }

    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        LOG.info("Creating direct consumer for queue " + queueName);
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

    public boolean isUseDirectProducer() { return this.useDirectProducer; }

    /**
     * Some description of this option, and what it does
     * TODO can remove?
     */
    public void setName(String name) {
        this.name = name;
    }

    
}
