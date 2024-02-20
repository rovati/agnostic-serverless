package ch.elca.rovl.queuecomponent.client.send;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import ch.elca.rovl.queuecomponent.util.ExchangeSerializer;

public class AWSQueueSenderClient implements QueueSenderClient {

    private static final Logger LOG = LoggerFactory.getLogger(AWSQueueSenderClient.class);

    String queueName;
    AmazonSQS client;
    String region;
    String queueUrl;

    public AWSQueueSenderClient(String queueName) {
        this.queueName = queueName;
        this.client = AmazonSQSClientBuilder
            .standard()
            .withRegion(Regions.EU_CENTRAL_1) // TODO extract from queue url
            .build();

        this.queueUrl = System.getenv("QUEUE_URL_" + queueName);
        if (this.queueUrl == null) {
            throw new IllegalStateException("Could not load url of queue " + queueName);
        }
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

        SendMessageRequest msg = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(jsonedExchange);
        LOG.info("Sending SQS message \'" + msg.toString() + "\'");
        client.sendMessage(msg);
        LOG.info("Message sent");
    }

    @Override
    public void stop() {
        client.shutdown();
    }
    
}
