package ch.elca.rovl.queuecomponent;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DirectQueueConsumer extends DefaultConsumer {

    QueueComponent component;
    String queueName;

    public DirectQueueConsumer(QueueEndpoint endpoint, Processor processor, String queueName) {
        super(endpoint, processor);
        this.component = (QueueComponent) endpoint.getComponent();
        this.queueName = queueName;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        component.addConsumer(queueName, this);
    }
    
}