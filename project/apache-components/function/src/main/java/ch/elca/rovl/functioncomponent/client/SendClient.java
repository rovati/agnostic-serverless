package ch.elca.rovl.functioncomponent.client;

import org.apache.camel.Exchange;

/**
 * Interface for clients used to send events to cloud functions.
 */
public interface SendClient {
    /**
     * Sends the given Exchange to the cloud function.
     * 
     * @param e exchange
     */
    public void send(Exchange e);

    /**
     * Stops the client.
     */
    public void stop();
}
