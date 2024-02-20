package ch.elca.rovl.functioncomponent.util;

import java.util.Optional;

public class PropertyResolver {

    public static TargetProvider getTargetProvider(Optional<String> prop) {
        if (prop.isPresent()) {
            switch(prop.get().toLowerCase()) {
                case "azure":
                    return TargetProvider.AZURE;
                case "aws":
                    return TargetProvider.AWS;
                default:
                    throw new IllegalArgumentException("PropertyResolver: Unsupported provider!");
            }
        } else {
            return null;
        }
    }

    public static TriggerType getTriggerType(Optional<String> prop) {
        if (prop.isPresent()) {
            switch(prop.get().toLowerCase()) {
                case "http":
                    return TriggerType.HTTP;
                case "queue":
                    return TriggerType.QUEUE;
                case "api":
                    return TriggerType.API;
                case "timer":
                    return TriggerType.TIMER;
                default:
                    throw new IllegalArgumentException("PropertyResolver: Unsupported trigger type!");
            }
        } else {
            return null;
        }
    }
}
