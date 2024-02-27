package ch.elca.rovl.queuecomponent.client;

import ch.elca.rovl.queuecomponent.client.send.AWSQueueSenderClient;
import ch.elca.rovl.queuecomponent.client.send.AzureQueueSenderClient;
import ch.elca.rovl.queuecomponent.client.send.QueueSenderClient;
import ch.elca.rovl.queuecomponent.util.TargetProvider;

/**
 * Factory of queue clients. It returns the client for the requested provider.
 */
public class QueueClientFactory {

    public static QueueSenderClient createSenderClient(String queueName, TargetProvider provider) {
        switch (provider) {
            case AZURE:
                return new AzureQueueSenderClient(queueName);
            case AWS:
                return new AWSQueueSenderClient(queueName);
            default:
                throw new IllegalArgumentException("Unsupported provider!");
        }
    }

}