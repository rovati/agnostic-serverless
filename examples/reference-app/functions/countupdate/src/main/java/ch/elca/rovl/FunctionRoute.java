package ch.elca.rovl;

import org.apache.camel.builder.RouteBuilder;

public class FunctionRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("queue:votesqueue")
            .log("In camel route with payload: ${body}")
            .setHeader("vote", simple("${body}"))
            .setBody(constant("UPDATE votes SET count = count + 1 WHERE name = :?vote"))
            .to("database:votesdb?useHeadersAsParameters=true");
    }
    
}
