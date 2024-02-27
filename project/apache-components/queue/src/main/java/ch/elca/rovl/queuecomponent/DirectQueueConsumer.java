package ch.elca.rovl.queuecomponent;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

/**
 * Queue consumer receiving messages locally from a queue producer used by the
 * function handler.
 */
public class DirectQueueConsumer extends DefaultConsumer {

    private final QueueComponent component;
    private final String queueName;

    public DirectQueueConsumer(QueueEndpoint endpoint, Processor processor, String queueName) {
        super(endpoint, processor);
        this.component = (QueueComponent) endpoint.getComponent();
        this.queueName = queueName;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        // register the consumer in the component, so that it can be discovered by the
        // producer
        component.addConsumer(queueName, this);
    }

}