package ch.elca.rovl.dsl.pipeline.templating;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.elca.rovl.dsl.pipeline.util.Constants;

/**
 * Data strcture to keep track of names of resources that have already been deployed. It writes
 * entries to a file that is read each time the pipeline is run.
 */
public class ResourceNameMemory {

    static final Logger LOG = LoggerFactory.getLogger("Templates gen");

    final Map<String, String> memory;
    final File memoryFile;

    /**
     * Constructor.
     * 
     * @throws IOException
     */
    ResourceNameMemory() throws IOException {
        memory = new HashMap<>();

        memoryFile = new File(Constants.DEPLOYMENT_MEMORY_FILE);
        if (!memoryFile.exists()) {
            FileUtils.createParentDirectories(memoryFile);
            memoryFile.createNewFile();
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(memoryFile));

            for (String line; (line = reader.readLine()) != null;) {
                String[] vals = line.split("=");
                if (vals.length != 2) {
                    // bad format, delete line
                    LOG.info(String.format("Skipping malformed memory entry: '%s'", line));
                } else {
                    memory.put(vals[0], vals[1]);
                }
            }

            reader.close();
        }
    }

    /**
     * Gets the cloud name of the given resource.
     * 
     * @param resName name of the resource
     * @return name of the corresponding resource in the cloud
     */
    public String get(String resName) {
        return memory.get(resName);
    }

    /**
     * Register the cloud name of a resource.
     * 
     * @param resName name of the resource
     * @param cloudResName name of the corresponding resource in the cloud
     * @throws IOException
     */
    public void addNewMemory(String resName, String cloudResName) throws IOException {
        memory.put(resName, cloudResName);
        FileUtils.writeStringToFile(memoryFile, resName + "=" + cloudResName + "\n", "utf8", true);
    }

}
