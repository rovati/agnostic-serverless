package ch.elca.rovl.dsl.resource.database;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.resource.Resource;

/**
 * Database resource.
 */
public final class Database extends Resource {

    private final String username;
    private final String password;
    private final DatabaseEngine engine;
    private final Map<DatabaseConfigType, Object> config;

    public Database(String name, String username, String password, DatabaseEngine engine) {
        super(name);
        this.username = username;
        this.password = password;
        this.engine = engine;
        this.config = new HashMap<>();
    }

    public void addConfig(Map<DatabaseConfigType, Object> config) {
        for (DatabaseConfigType configType : config.keySet()) {
            this.config.put(configType, config.get(configType));
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseEngine getEngine() {
        return engine;
    }

    public Map<DatabaseConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Database[name=%s]", name);
    }

    // NOTE expand to support more configuration
    public enum DatabaseConfigType {
        WORKLOAD_TYPE, SIZE_GB
    }

    public enum DatabaseSize {
        MIN, MAX
    }
    
}
