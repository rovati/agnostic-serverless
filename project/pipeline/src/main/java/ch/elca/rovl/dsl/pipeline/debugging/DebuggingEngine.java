package ch.elca.rovl.dsl.pipeline.debugging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedDatabase;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.pipeline.util.TriggerType;

public class DebuggingEngine {
    private static final Logger LOG = LoggerFactory.getLogger("Debug Config");
    private static final int MIN_PORT_NUMBER = 49152;
    private static final int MAX_PORT_NUMBER = 65534;

    final Random rng = new Random();

    final List<LinkedFunction> functions;
    final List<LinkedDatabase> databases;
    final Map<String, String> dummyProjects;
    final Map<String, Integer> ports;

    final DummyDBHelper dbHelper;

    public DebuggingEngine(Map<String, LinkedResource> linkedResource) {
        this.functions = new ArrayList<>();
        this.databases = new ArrayList<>();
        this.dummyProjects = new HashMap<>();
        this.ports = new HashMap<>();
        this.dbHelper = new DummyDBHelper();

        for (LinkedResource lr : linkedResource.values()) {
            if (lr instanceof LinkedFunction) {
                functions.add((LinkedFunction) lr);
            }
            if (lr instanceof LinkedDatabase) {
                databases.add((LinkedDatabase) lr);
            }
        }
    }

    public void choosePortForDatabases() {
        List<Integer> selectedPorts = new ArrayList<>();

        for (LinkedDatabase db : databases) {
            int port;
            do {
                port = rng.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
            } while (!isPortAvailable(port) && !selectedPorts.contains(port));

            try {
                dummyProjects.put(db.getName(), dbHelper.createDummyProject(db.getName(), port));
            } catch (IOException e) {
                LOG.error(String.format("Failed to create dummy project for database '%s'", db.getName()), e);
            }
            ports.put(db.getName(), port);
        }
    }

    public void startDummyDatabases() {
        ExecutorService executor = Executors.newCachedThreadPool();

        Map<Future<?>, String> startupTasks = new HashMap<>();

        for (String dbName : dummyProjects.keySet()) {
            startupTasks.put(executor.submit(() -> {
                String cmd = " & mvn quarkus:dev -Ddebug=false";
                Process p = null;
                try {
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", " cd " + dummyProjects.get(dbName), cmd);
                    // write output to log file
                    pb.redirectOutput(new File(Constants.LOGS_DIR + "start-dummy-db-" + dbName + ".txt"));
                    
                    // run command
                    p = pb.start();

                    p.waitFor(30, TimeUnit.SECONDS);
                    p.destroyForcibly();
                } catch (IOException | InterruptedException e) {
                    if (p != null) {
                        p.destroyForcibly();
                    }
                    e.printStackTrace();
                    throw new RuntimeException(String.format(
                            "Failed to start dummy dev service for db '%s'", dbName));
                }
            }), dbName);
        }

        boolean startupHasFailed = false;

        // wait until all tasks are done
        for (Future<?> res : startupTasks.keySet()) {
            try {
                res.get();
            } catch (ExecutionException | InterruptedException e) {
                startupHasFailed = true;
                LOG.error(String.format("Startup failed for database '%s'",
                        startupTasks.get(res)), e);
            }
        }

        executor.shutdown();

        if (startupHasFailed) {
            throw new IllegalStateException("Startup of Dev Services for some databases has failed.");
        }
    }

    public void writeDebugConfig() throws IOException {
        // write config files to each functton project with links and function name
        for (LinkedFunction fn : functions) {
            File appPropertiesFile = new File(
                    fn.getFunction().getPathToProject() + fn.getName() + "/src/main/resources/application.properties");
            FileReader fReader = new FileReader(appPropertiesFile);
            BufferedReader reader = new BufferedReader(fReader);
            List<String> modifiedContent = new ArrayList<>();

            Boolean insideGeneratedBlock = false;

            // remove previous data
            for (String line; (line = reader.readLine()) != null;) {
                if (line.contains("# GENERATED - DO NOT EDIT")) {
                    insideGeneratedBlock = true;
                }
                if (line.contains("# END GENERATED")) {
                    insideGeneratedBlock = false;
                    continue;
                }

                if (insideGeneratedBlock && (line.contains("function.name") ||
                        line.contains("function.trigger") ||
                        line.contains("quarkus.datasource") ||
                        line.contains("# GENERATED - DO NOT EDIT") ||
                        line.contains("# END GENERATED"))) {
                    continue;
                }
                modifiedContent.add(line);
            }

            fReader.close();
            reader.close();

            // add config
            modifiedContent.add("# GENERATED - DO NOT EDIT");
            modifiedContent.add("function.name=" + fn.getName());

            String trigger;
            if (fn.getTriggerType() == TriggerType.HTTP) {
                if (fn.getTrigger() != null) // rest trigger
                    trigger = "api";
                else
                    trigger = "http";
            } else {
                trigger = fn.getTriggerType().toString();
            }
            modifiedContent.add("function.trigger=" + trigger);

            if (fn.hasDatabaseOutput()) {
                for (LinkedResource lr : fn.getOutput()) {
                    if (lr instanceof LinkedDatabase) {
                        String kind = String.format("quarkus.datasource.%s.db-kind=postgresql", lr.getName());
                        String username = String.format("quarkus.datasource.%s.username=quarkus", lr.getName());
                        String password = String.format("quarkus.datasource.%s.password=quarkus", lr.getName());
                        String url = String.format(
                                "quarkus.datasource.%s.jdbc.url=jdbc:postgresql://localhost:%d/quarkus", lr.getName(),
                                ports.get(lr.getName()));

                        modifiedContent.add(kind);
                        modifiedContent.add(username);
                        modifiedContent.add(password);
                        modifiedContent.add(url);
                    }
                }
            }

            modifiedContent.add("# END GENERATED");
            FileUtils.writeLines(appPropertiesFile, modifiedContent);
        }
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    private static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

}
