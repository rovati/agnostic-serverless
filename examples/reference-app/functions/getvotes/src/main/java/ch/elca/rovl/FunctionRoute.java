package ch.elca.rovl;

import org.apache.camel.builder.RouteBuilder;

public class FunctionRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("function:trigger")
            .log("In camel route with payload: ${body}")
            .setBody(constant("SELECT * FROM votes ORDER BY count DESC"))
            .to("database:votesdb")
            .convertBodyTo(String.class);
    }
    
}
