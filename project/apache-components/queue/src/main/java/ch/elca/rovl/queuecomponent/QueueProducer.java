package ch.elca.rovl.queuecomponent;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.queuecomponent.client.QueueClientFactory;
import ch.elca.rovl.queuecomponent.client.send.QueueSenderClient;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

public class QueueProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(QueueProducer.class);
    
    String queueName;
    QueueSenderClient client;

    public QueueProducer(Endpoint endpoint, String queueName, TargetProvider provider) {
        super(endpoint);
        this.queueName = queueName;
        this.client = QueueClientFactory.createSenderClient(queueName, provider);
        LOG.info("Created queue producer for queue \'" + queueName + "\'");
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        LOG.info("Sending message \'" + exchange.getIn().getBody(String.class) + "\'");
        client.send(exchange);
        LOG.info("Message sent.");
    }

}
