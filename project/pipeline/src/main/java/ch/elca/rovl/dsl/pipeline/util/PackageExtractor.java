package ch.elca.rovl.dsl.pipeline.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to extract packages from Java files.
 */
public class PackageExtractor {
    
    /**
     * Gets the package from a Java file.
     * 
     * @param javaFile java file
     * @return package of the Java file
     * @throws IOException
     */
    public static String getFilePackage(File javaFile) throws IOException {
        FileReader fReader = new FileReader(javaFile);
        BufferedReader reader = new BufferedReader(fReader);

        String filePackage = "";

        for (String line; (line = reader.readLine()) != null;) {
            // if line is model version, append parent tag
            if (line.trim().startsWith("package ")) {
                filePackage = line.trim();
            }
        }

        fReader.close();
        reader.close();

        if (filePackage.isBlank())
            throw new IllegalStateException("File did not specify a package!");
        else
            return filePackage;
    }
}
