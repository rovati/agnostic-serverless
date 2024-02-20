package ch.elca.rovl.dsl.pipeline.linking.resource;

import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslQueue;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Object representing a cloud queue with its target provider chosen by the user. It contains a
 * reference to the queue object of the previous pipeline step.
 */
public final class LinkedQueue extends LinkedResource {

    public LinkedQueue(DslQueue queue, Provider provider) {
        super(queue, provider);
    }

    public DslQueue getQueue() {
        return (DslQueue) dslResource;
    }

    @Override
    public String toString() {
        return String.format("LinkedQueue[name=%s, provider=%s]", dslResource.getName(), provider);
    }

}
