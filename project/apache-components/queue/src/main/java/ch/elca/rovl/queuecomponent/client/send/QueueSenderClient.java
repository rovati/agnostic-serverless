package ch.elca.rovl.queuecomponent.client.send;

import org.apache.camel.Exchange;

public interface QueueSenderClient {
    public void send(Exchange e);
    public void stop();
}

