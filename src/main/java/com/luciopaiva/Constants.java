package com.luciopaiva;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    /** How long to let the process sleep while waiting for a select() */
    public static final int SELECT_TIMEOUT_IN_MILLIS = 50;
    public static final int SERVER_PORT = 3000;
    /** must be a power of two and greater than a long */
    public static final int PACKET_SIZE_IN_BYTES = 1024;
    public static final int DEFAULT_NUMBER_OF_CLIENTS = 10;
    public static final long METRICS_REPORT_PERIOD_IN_MILLIS = 1000;
    public static final long SEND_PERIOD_IN_MILLIS = 200;
    /** When using the uniform sending strategy, we need a routine looping in high frequency to be able to cope with
     *  the distribution of clients across the sending window. */
    public static final int UNIFORM_STRATEGY_SELECT_TIMEOUT_IN_MILLIS = 20;
    /** The send window will be divided into this many slots. The more there is, the finer the grain of the
     *  distribution will be. For example, if the send window is of 200 ms (i.e., all clients must receive exactly one
     *  packet each 200 ms), dividing it by 100 slots means we'll have an opportunity to send something every 2 ms. */
    static final int UNIFORM_STRATEGY_NUMBER_OF_SLOTS = 10;

    public enum SendStrategy {
        Burst("burst"),
        Uniform("uniform");

        private static Map<String, SendStrategy> strategyByName = new HashMap<>();
        static {
            strategyByName.put(Burst.name, Burst);
            strategyByName.put(Uniform.name, Uniform);
        }

        String name;

        SendStrategy(String name) {
            this.name = name;
        }

        public static SendStrategy getByName(String name) {
            return strategyByName.get(name);
        }

        public static String getValidNames() {
            return strategyByName.keySet().toString();
        }
    }
}
