package ch.elca.rovl.dsl.resource.function;

public enum TriggerType {
    REST("rest-api"), QUEUE("queue"), TIMER("timer");

    private String name;

    private TriggerType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
