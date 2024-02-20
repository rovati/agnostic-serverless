package ch.elca.rovl.dsl.pipeline.util;

/**
 * Class representing a tag for configuration data required by a deployed resource.
 */
public class RequiredData {

    private final Type type;
    private final String resourceName;

    public RequiredData(Type type, String resourceName) {
        this.type = type;
        this.resourceName = resourceName;
    }

    public Type getType() {
        return type;
    }

    public String getResourceName() {
        return resourceName;
    }

    /**
     * Possible types of requried data.
     */
    public static enum Type {
        QUEUE_SEND, QUEUE_LISTEN, FUNCTION_URL, QUEUE_TRIGGER, DATABASE_CONNECTION, SHARED_KEY
    }

    @Override
    public String toString() {
        return String.format("RequiredData[type=%s, resource=%s]", type, resourceName);
    }
}
