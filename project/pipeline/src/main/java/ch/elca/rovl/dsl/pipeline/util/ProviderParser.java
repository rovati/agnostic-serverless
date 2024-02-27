package ch.elca.rovl.dsl.pipeline.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class ProviderParser {
    
    File providersFile;
    Map<String,Provider> parsedProviders;
    Provider defaultProvider;

    public ProviderParser(String fileName) throws URISyntaxException {
        this.parsedProviders = new HashMap<>();
        this.providersFile = new File(fileName);
    }

    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(providersFile));
        
        for (String line; (line = reader.readLine()) != null;) {
            // ignore comments
            if (line.trim().startsWith("#"))
                continue;

            int separatorIdx = line.indexOf('=');
            if (separatorIdx == -1 || separatorIdx == line.length() - 1) {
                reader.close();
                throw new InvalidParameterException(String.format(
                    "The key '%s' is missing a value!", line));
            }

            String key = line.substring(0,separatorIdx);
            String value = line.substring(separatorIdx + 1);

            if (key.equals("default"))
                defaultProvider = parseProviderValue(value);
            else
                parsedProviders.put(key, parseProviderValue(value));
        }

        reader.close();
    }

    // TODO log when default value is set
    public Provider getProviderOf(String name) {
        Provider p = parsedProviders.get(name);
        if (p == null && defaultProvider != null)
            p = defaultProvider;
        return p;
    }

    private Provider parseProviderValue(String value) {
        switch(value) {
            case "aws":
            case "AWS":
                return Provider.AWS;
            case "azure":
            case "Azure":
                return Provider.AZURE;
            default:
                throw new IllegalArgumentException(String.format(
                    "Provider %s is not supported!", value));
        }
    }
}
