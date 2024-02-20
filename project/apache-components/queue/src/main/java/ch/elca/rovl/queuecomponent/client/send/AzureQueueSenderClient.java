package ch.elca.rovl.queuecomponent.client.send;

import java.io.IOException;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import ch.elca.rovl.queuecomponent.util.ExchangeSerializer;

// messaging client
public class AzureQueueSenderClient implements QueueSenderClient {

    private static final Logger LOG = LoggerFactory.getLogger(AzureQueueSenderClient.class);

    String queueName;
    String connString;
    ServiceBusSenderClient senderClient;

    public AzureQueueSenderClient(String queueName) {
        this.queueName = queueName;
        
        this.connString = System.getenv("CONN_STRING_" + queueName);
        if (this.connString == null) {
            throw new IllegalStateException("Could not load access key to queue " + queueName);
        }

        createClient();
    }

    @Override
    public void send(Exchange exchange) {
        String jsonedExchange;
        
        try {
            jsonedExchange = ExchangeSerializer.serialize(exchange);
        } catch (IOException e) {
            LOG.error("Failed to create JSON for Exchange! Dropping message", e);
            return;
        }

        ServiceBusMessage msg = new ServiceBusMessage(jsonedExchange);
        LOG.info("Sending SB message \'" + msg.toString() + "\'");
        senderClient.sendMessage(msg);
        LOG.info("Message sent");
    }

    @Override
    public void stop() {
        senderClient.close();
    }

    private void createClient() {
        senderClient = new ServiceBusClientBuilder()
            .connectionString(connString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .sender()
            .queueName(queueName)
            .buildClient();
    }
    
}
