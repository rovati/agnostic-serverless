package ch.elca.rovl.dsl.pipeline.util;

import java.util.Random;

/**
 * Utility class to generate Ids for cloud resource names
 */
public final class IdGenerator {
    private static IdGenerator instance;
    private static int MAX = 100000000;
    private static int MIN = 10000000;
    private Random rng;

    private IdGenerator() {
        this.rng = new Random();
    }

    public static IdGenerator get() {
        if (instance == null) {
            instance = new IdGenerator();
        }

        return instance;
    }

    /**
     * Generates an Id between 10000000 and 99999999.
     * <p>
     * Not supposed to be a uniform distribution on the interval, but just an helper to get a
     * somewhat random integer in range.
     * 
     * @return random integer between 10000000 and 99999999
     */
    public int generate() {
        int next = rng.nextInt(MAX);
        next = next < MIN ? next + MIN : next;

        return next;
    }

}
