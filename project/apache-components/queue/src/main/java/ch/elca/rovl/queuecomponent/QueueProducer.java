package ch.elca.rovl.queuecomponent;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import ch.elca.rovl.queuecomponent.client.QueueClientFactory;
import ch.elca.rovl.queuecomponent.client.send.QueueSenderClient;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

/**
 * Queue producer. It pushes Exchanges to the cloud queue.
 */
public class QueueProducer extends DefaultProducer {
    
    private final QueueSenderClient client;

    public QueueProducer(Endpoint endpoint, String queueName, TargetProvider provider) {
        super(endpoint);
        // get client for the platform the queue is provisioned on
        this.client = QueueClientFactory.createSenderClient(queueName, provider);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        client.send(exchange);
    }

}
