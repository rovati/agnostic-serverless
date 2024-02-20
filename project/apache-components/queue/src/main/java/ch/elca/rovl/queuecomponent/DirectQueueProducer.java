package ch.elca.rovl.queuecomponent;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

public class DirectQueueProducer extends DefaultProducer {

    QueueComponent component;
    QueueEndpoint endpoint;
    DirectQueueConsumer consumer;
    String queueName;

    public DirectQueueProducer(QueueEndpoint endpoint, String queueName) {
        super(endpoint);
        this.endpoint = endpoint;
        this.component = (QueueComponent) endpoint.getComponent();
        this.queueName = queueName;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        consumer = endpoint.getConsumer();
        if (consumer == null) {
            throw new IllegalStateException("Couldn't get the consumer");
        }

        consumer.getProcessor().process(exchange);
    }
    
}