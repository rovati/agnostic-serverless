package ch.elca.rovl.queuecomponent;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

/**
 * Queue producer exchanging messages locally with a queue consumer.
 * <p>
 * Processing an Exchange will simply pass it to the corresponding consumer.
 */
public class DirectQueueProducer extends DefaultProducer {

    QueueEndpoint endpoint;
    private DirectQueueConsumer consumer;

    public DirectQueueProducer(QueueEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // get the corresponding consumer and let the consumer process the exchange
        consumer = endpoint.getConsumer();
        if (consumer == null) {
            throw new IllegalStateException("Couldn't get the consumer");
        }
        consumer.getProcessor().process(exchange);
    }

}