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

/**
 * Client used to push messages to a queue provisioned on AWS SQS.
 * <p>
 * NOTE supports queues only in region EU_CENTRAL_1
 * TODO get region from properties or env var, or extract from queue url
 */
public class AWSQueueSenderClient implements QueueSenderClient {

    private static final Logger LOG = LoggerFactory.getLogger(AWSQueueSenderClient.class);

    private final AmazonSQS client;
    private final String queueUrl;

    public AWSQueueSenderClient(String queueName) {
        // init client
        this.client = AmazonSQSClientBuilder
            .standard()
            .withRegion(Regions.EU_CENTRAL_1)
            .build();

        // get queue url from environment
        this.queueUrl = System.getenv("QUEUE_URL_" + queueName);
        if (this.queueUrl == null) {
            throw new IllegalStateException("Could not load url of queue " + queueName);
        }
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

        // send message to AWS SQS queue
        LOG.info("Sending SQS message.");
        SendMessageRequest msg = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(jsonedExchange);
        client.sendMessage(msg);
        LOG.info("Message sent.");
    }

    @Override
    public void stop() {
        client.shutdown();
    }
    
}
