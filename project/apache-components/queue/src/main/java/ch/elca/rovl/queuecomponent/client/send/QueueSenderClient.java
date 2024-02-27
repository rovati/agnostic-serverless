package ch.elca.rovl.queuecomponent.client.send;

import org.apache.camel.Exchange;

/**
 * Queue client interface used to push messages to cloud queues.
 */
public interface QueueSenderClient {
    /**
     * Pushes the given Exchange to the cloud queue.
     * @param e Exchange
     */
    public void send(Exchange e);
    /**
     * Stops the client.
     */
    public void stop();
}

