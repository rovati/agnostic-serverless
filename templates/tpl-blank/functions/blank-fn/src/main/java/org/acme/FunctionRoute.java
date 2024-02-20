package org.acme;

import org.apache.camel.builder.RouteBuilder;

/**
 * Function behaviour definition.
 * Sets the body of the HTTP response to "Hello World!".
 */
public class FunctionRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // TODO configure route

        /* configuration example

        from("function:trigger")
            .setBody(constant("Hello World!"));

        */
    }
    
}
