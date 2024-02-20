package ch.elca.rovl.dsl.pipeline.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * Utility class to load a file into a String from the classpath resources.
 */
public class ResourceLoader {

    /**
     * Loads the classpath resource with the given name into a String and returns it.
     * 
     * @param filename name of the file in classpath resources
     * @return a string with the content of the file
     * @throws IOException
     */
    public static String load(String filename, Logger log) throws IOException {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(filename)) {
            if (is == null) {
                log.error("Resource " + filename + " not found.");
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
