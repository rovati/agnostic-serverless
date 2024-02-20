package ch.elca.rovl.dsl.pipeline.util;

/**
 * List of supported cloud function trigger types.
 */
public enum TriggerType {
    HTTP("http"), QUEUE("queue"), TIMER("timer");

    private String name;

    private TriggerType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
