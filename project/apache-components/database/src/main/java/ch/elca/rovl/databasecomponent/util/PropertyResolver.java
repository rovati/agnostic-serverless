package ch.elca.rovl.databasecomponent.util;

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
}

