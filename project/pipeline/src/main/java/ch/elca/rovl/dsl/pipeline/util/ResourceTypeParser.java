package ch.elca.rovl.dsl.pipeline.util;

/**
 * Utility class to parse resource types from their string names.
 */
public class ResourceTypeParser {

    /**
     * Returns the resource type enum value from its string name.
     * 
     * @param raw name of the resource type
     * @return resource type
     */
    public static ResourceType parseType(String raw) {
        switch (raw) {
            case "function":
                return ResourceType.FUNCTION;
            case "queue":
                return ResourceType.QUEUE;
            case "database":
                return ResourceType.DATABASE;
            default:
                throw new IllegalArgumentException(
                        String.format("Raw resource type '%s' is not recognized", raw));
        }
    }
}
