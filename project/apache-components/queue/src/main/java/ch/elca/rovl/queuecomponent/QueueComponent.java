package ch.elca.rovl.queuecomponent;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import ch.elca.rovl.queuecomponent.util.PropertyResolver;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

/**
 * Camel component for the queue building block.
 * <p>
 * The endpoint URI is specified as "queue:queueName"
 */
@Component("queue")
public class QueueComponent extends DefaultComponent {


    Map<String, DirectQueueConsumer> consumers = new HashMap<>();

    public QueueComponent() {
    }

    public QueueComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // name of the property indicating on which platform the sink queue is
        // prpovisioned on
        String providerProperty = String.format("queue.%s.provider", remaining);

        TargetProvider provider = PropertyResolver.getTargetProvider(
                getCamelContext().getPropertiesComponent().resolveProperty(
                        providerProperty));

        // if propoerties does not contain queue target platform, the component is being
        // used locally
        if (provider == null) {
            // debug mode, use kafka endpoint
            return getCamelContext().getEndpoint("kafka:" + remaining);
        } else {
            return new QueueEndpoint(uri, remaining, this);
        }

    }

    /**
     * Used by endpoints to register a created consumer. The consumer is used
     * locally to push messsages to the Camel route.
     * 
     * @param queueName
     * @param consumer
     */
    public void addConsumer(String queueName, DirectQueueConsumer consumer) {
        synchronized (consumers) {
            if (consumers.putIfAbsent(queueName, consumer) != null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot add a second consumer to the same endpoint for queue '%s'", queueName));
            }

            consumers.notifyAll();
        }
    }

    /**
     * Used by local producers created by the function handler to push the
     * triggering event to the Camel route.
     * 
     * @param queueName
     * @return
     * @throws InterruptedException
     */
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
