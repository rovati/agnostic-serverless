package org.acme;

import ch.elca.rovl.dsl.pipeline.PipelineEngine;

/**
 * Main class to run the pipeline.
 *
 */
public class App  
{
    public static void main( String[] args )
    {
        PipelineEngine engine = new PipelineEngine(new MyAppResources());
        engine.run(args);
    }
}
