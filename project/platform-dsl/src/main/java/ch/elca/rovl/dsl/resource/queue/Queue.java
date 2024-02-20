package ch.elca.rovl.dsl.resource.queue;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.resource.Resource;

public final class Queue extends Resource {

    private final Map<QueueConfigType, Object> config;

    public Queue(String name) {
        super(name);
        this.config = new HashMap<>();
    }

    public void addConfig(Map<QueueConfigType, Object> config) {
        for (QueueConfigType configType : config.keySet()) {
            this.config.put(configType, config.get(configType));
        }
    }

    public Map<QueueConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Queue[name=%s]", name);
    }

    public enum QueueConfigType {
        MAX_SIZE
    }
    
}
