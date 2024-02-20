package ch.elca.rovl;

import ch.elca.rovl.dsl.pipeline.PipelineEngine;
import ch.elca.rovl.layout.MyAppResources;

/**
 * Hello cloud!
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
