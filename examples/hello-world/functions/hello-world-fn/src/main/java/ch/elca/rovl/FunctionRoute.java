package ch.elca.rovl;

import org.apache.camel.builder.RouteBuilder;

/**
 * Function behaviour definition.
 * Sets the body of the HTTP response to "Hello World!".
 */
public class FunctionRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("function:trigger")
            .setBody(constant("Hello World!"));
    }
    
}
