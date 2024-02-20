package ch.elca.rovl;

import org.apache.camel.builder.RouteBuilder;

public class FunctionRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("function:trigger")
            .log("In camel route with payload: ${body}")
            .process(new VoteProcessor())
            .choice()
                .when(body().contains("unrecognized"))
                .log("Vote value is not valid! Ignoring vote.")
            .otherwise()
                .log("Vote received for: ${body}")
                .to("queue:votesqueue");
    }
    
}
