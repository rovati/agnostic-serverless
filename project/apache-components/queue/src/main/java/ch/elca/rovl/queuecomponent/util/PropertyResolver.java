package ch.elca.rovl.queuecomponent.util;

import java.util.Optional;

/**
 * Utility class to parse a platform provider given a loaded property
 */
public class PropertyResolver {
    /**
     * Returns the provider specified by the property, or null if the property does
     * not exist.
     * 
     * @param prop
     * @return
     */
    public static TargetProvider getTargetProvider(Optional<String> prop) {
        if (prop.isPresent()) {
            switch (prop.get().toLowerCase()) {
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
}