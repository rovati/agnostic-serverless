package ch.elca.rovl.dsl.pipeline.templating.resource;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedQueue;

/**
 * Object representing a cloud queue with its target provider chosen by the user and the name of the
 * corresponding resource in the cloud. It contains a reference to the queue object of the previous
 * pipeline step.
 */
public final class DeployableQueue extends DeployableResource {

    public DeployableQueue(LinkedQueue q) {
        super(q);
    }

    public LinkedQueue getQueue() {
        return (LinkedQueue) linkedResource;
    }

    @Override
    public String toString() {
        return String.format("DeployableQueue[name=%s, provider=%s, cloud-name=%s]", getName(),
                getProvider(), cloudName);
    }

}
