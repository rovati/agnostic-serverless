package ch.elca.rovl.dsl.pipeline.templating;

import java.util.Arrays;
import java.util.List;

public class TemplatingConstants {

    public static final List<String> JDBC_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>org.apache.camel.quarkus</groupId>",
                "\t\t<artifactId>camel-quarkus-jdbc</artifactId>", "\t\t<version>3.6.0</version>",
                "\t</dependency>");
    public static final List<String> POSTGRES_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>io.quarkus</groupId>",
                "\t\t<artifactId>quarkus-jdbc-postgresql</artifactId>",
                "\t\t<version>3.6.4</version>", "\t</dependency>");
    public static final List<String> COMMONS_DBCP_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>commons-dbcp</groupId>",
                "\t\t<artifactId>commons-dbcp</artifactId>", "\t\t<version>1.4</version>",
                "\t</dependency>");
    public static final List<String> COMMONS_POOL_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>commons-pool</groupId>",
                "\t\t<artifactId>commons-pool</artifactId>", "\t\t<version>1.5.4</version>",
                "\t</dependency>");
    public static final List<String> JACKSON_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>com.fasterxml.jackson.jr</groupId>",
                "\t\t<artifactId>jackson-jr-objects</artifactId>", "\t\t<version>2.16.1</version>",
                "\t</dependency>");
    public static final List<String> JJWT_API_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>io.jsonwebtoken</groupId>",
                "\t\t<artifactId>jjwt-api</artifactId>", "\t\t<version>0.12.3</version>",
                "\t</dependency>");
    public static final List<String> JJWT_IMPL_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>io.jsonwebtoken</groupId>",
                "\t\t<artifactId>jjwt-impl</artifactId>", "\t\t<version>0.12.3</version>",
                "\t\t<scope>runtime</scope>",
                "\t</dependency>");
    public static final List<String> JJWT_JACKSON_TAG = Arrays.asList("\t<dependency>", "\t\t<groupId>io.jsonwebtoken</groupId>",
                "\t\t<artifactId>jjwt-jackson</artifactId>", "\t\t<version>0.12.3</version>",
                "\t\t<scope>runtime</scope>",
                "\t</dependency>");

    public static final String DATABASE_PROPERTY = "database.%s.provider=%s\n";
    public static final String QUEUE_PROPERTY = "queue.%s.provider=%s\n";
    public static final String FUNCTION_PROPERTY = "function.%s.provider=%s\n";

    public static final String POM_NAME = "pom.xml";
    public static final String APP_PROPERTIES_NAME = "application.properties";
    public static final String HANDLER_NAME = "TriggerHandler.java";

    public static final String AWS_DB_IMPORTS = """
        import org.apache.commons.dbcp.BasicDataSource;
        import jakarta.inject.Named;
        import jakarta.enterprise.inject.Produces;
        """;

    public static final String AWS_DB_DATASOURCE_BLOCK = """
        \t@Produces
        \t@Named("%s")
        \tpublic BasicDataSource %sDatasource() {
        \t    BasicDataSource ds = new BasicDataSource();
        \t    ds.setUsername(System.getenv("PSQL_USER_%s"));
        \t    ds.setDriverClassName("org.postgresql.Driver");
        \t    ds.setPassword(System.getenv("PSQL_PWD_%s"));
        \t    ds.setUrl(System.getenv("PSQL_URL_%s"));
        \t    ds.addConnectionProperty("sslmode", "require");
        \t    return ds;
        \t}
        """;

    public static final String AZURE_DB_IMPORTS = """
        import org.apache.commons.dbcp.BasicDataSource;
        import java.util.List;
            """;

    public static final String AZURE_REGISTER_DBS = """
        \t\t\tList<String> dbs = List.of(%s);
        \t\t\tfor (String dbName : dbs) {
        \t\t\t\tcamelContext.getRegistry().bind(dbName, setupDataSource(dbName));
        \t\t\t}
            """;

    public static final String AZURE_DATASOURCE_METHOD = """
        \tprivate static BasicDataSource setupDataSource(String dbName) {
        \t\tBasicDataSource ds = new BasicDataSource();
        \t\tds.setUsername(System.getenv("PSQL_USER_" + dbName));
        \t\tds.setDriverClassName("org.postgresql.Driver");
        \t\tds.setPassword(System.getenv("PSQL_PWD_" + dbName));
        \t\tds.setUrl(System.getenv("PSQL_URL_" + dbName));
        \t\tds.addConnectionProperty("sslmode", "require");
        \t\treturn ds;
        \t}
            """;
}
