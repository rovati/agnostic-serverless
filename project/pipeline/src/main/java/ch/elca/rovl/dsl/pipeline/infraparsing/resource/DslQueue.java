package ch.elca.rovl.dsl.pipeline.infraparsing.resource;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.resource.queue.Queue;
import ch.elca.rovl.dsl.resource.queue.Queue.QueueConfigType;

/**
 * Object representing a cloud queue. It contains configuration specified through the infrastructure
 * DSL.
 */
public final class DslQueue extends DslResource {

    private Map<QueueConfigType, Object> config;

    /**
     * Object constructor. The parameters are mandatory configuration required to deploy the
     * database on any provider.
     * 
     * @param name unique name of the queue
     */
    public DslQueue(String name) {
        super(name);
        this.config = new HashMap<>();
    }

    public static DslQueue fromApiQueue(Queue q) {
        DslQueue dslQ = new DslQueue(q.getName());
        dslQ.setConfig(q.getConfig());
        return dslQ;
    } 

    /**
     * Registers optional queue configuration data.
     * 
     * @param config
     */
    public void setConfig(Map<QueueConfigType, Object> config) {
        this.config = config;
    }

    public Map<QueueConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Queue[name=%s]", name);
    }

}
