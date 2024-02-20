package ch.elca.rovl.queuecomponent;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.queuecomponent.util.PropertyResolver;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

@Component("queue")
public class QueueComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(QueueComponent.class);

    Map<String,DirectQueueConsumer> consumers = new HashMap<>();

    public QueueComponent() {}

    public QueueComponent(CamelContext context) {
        super(context);
    }
    
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LOG.info("Creating queue endpoint with name " + remaining + " and parameters " + parameters.toString());

        String providerProperty = String.format("queue.%s.provider", remaining);
        
        TargetProvider provider = PropertyResolver.getTargetProvider(
            getCamelContext().getPropertiesComponent().resolveProperty(
                providerProperty));

        if (provider == null) {
            LOG.info("No provider settings found, using kafka endpoint");
            return getCamelContext().getEndpoint("kafka:" + remaining);
        } else {
            return new QueueEndpoint(uri, remaining, this);
        }

    }

    public void addConsumer(String queueName, DirectQueueConsumer consumer) {
        synchronized (consumers) {
            if (consumers.putIfAbsent(queueName, consumer) != null) {
                throw new IllegalArgumentException(String.format(
                    "Cannot add a second consumer to the same endpoint for queue '%s'", queueName));
            }

            consumers.notifyAll();
        }
    }

    public DirectQueueConsumer getConsumer(String queueName) throws InterruptedException {
        synchronized (consumers) {
            DirectQueueConsumer consumer = consumers.get(queueName);

            if (consumer == null) {
                // try a second time after a while
                consumers.wait(1000);
                consumer = consumers.get(queueName);

                if (consumer == null) {
                    throw new IllegalStateException(String.format(
                        "Failed to retrieve direct consumer for queue '%s", queueName));
                }
            }

            return consumer;
        }
    }

}
