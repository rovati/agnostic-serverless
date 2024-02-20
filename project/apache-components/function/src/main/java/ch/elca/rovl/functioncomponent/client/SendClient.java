package ch.elca.rovl.functioncomponent.client;

import org.apache.camel.Exchange;

public interface SendClient {
    public void send(Exchange e);
    public void stop();
}
