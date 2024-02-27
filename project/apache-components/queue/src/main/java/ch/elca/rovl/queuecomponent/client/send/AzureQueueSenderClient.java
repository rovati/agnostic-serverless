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

/**
 * Client used to push messages to a queue provisioned on Azure Service Bus.
 */
public class AzureQueueSenderClient implements QueueSenderClient {

    private static final Logger LOG = LoggerFactory.getLogger(AzureQueueSenderClient.class);

    private final String connString;
    private final ServiceBusSenderClient senderClient;

    public AzureQueueSenderClient(String queueName) {
        // get connection string for the Service Bus queue 
        this.connString = System.getenv("CONN_STRING_" + queueName);
        if (this.connString == null) {
            throw new IllegalStateException("Could not load access key to queue " + queueName);
        }

        // initialize client for the queue on Service Bus
        this.senderClient = new ServiceBusClientBuilder()
            .connectionString(connString)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
            .sender()
            .queueName(queueName)
            .buildClient();
    }

    @Override
    public void send(Exchange exchange) {
        // serialize exchange
        String jsonedExchange;
        try {
            jsonedExchange = ExchangeSerializer.serialize(exchange);
        } catch (IOException e) {
            LOG.error("Failed to create JSON for Exchange! Dropping message", e);
            return;
        }

        // send message to Service Bus queue
        LOG.info("Sending Service Bus message.");
        ServiceBusMessage msg = new ServiceBusMessage(jsonedExchange);
        senderClient.sendMessage(msg);
        LOG.info("Message sent.");
    }

    @Override
    public void stop() {
        senderClient.close();
    }

}
