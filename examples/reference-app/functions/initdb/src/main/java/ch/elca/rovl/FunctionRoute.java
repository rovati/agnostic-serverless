package ch.elca.rovl;

import org.apache.camel.builder.RouteBuilder;

public class FunctionRoute extends RouteBuilder {

    private static final String CREATE_TABLE_QUERY = """
        CREATE TABLE IF NOT EXISTS votes(
            name varchar(10) PRIMARY KEY,
            count integer NOT NULL
        );
        """;
                
    private static final String INSERT_VALUES_QUERY =  """
        INSERT INTO votes VALUES 
            ('intellij', 0),
            ('vscode', 0),
            ('eclipse', 0),
            ('bluej', 0),
            ('netbeans', 0),
            ('other', 0);
            """;

    @Override
    public void configure() throws Exception {
        from("function:trigger")
            .log("In camel route with payload: ${body}")
            .setBody(constant(CREATE_TABLE_QUERY))
            .to("database:votesdb")
            .setBody(constant(INSERT_VALUES_QUERY))
            .to("database:votesdb");
    }
    
}
