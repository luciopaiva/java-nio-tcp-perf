package com.luciopaiva;

public class Constants {

    /** How long to let the process sleep while waiting for a select() */
    public static final int SELECT_TIMEOUT_IN_MILLIS = 100;
    public static final int SERVER_PORT = 3023;
    /** must be a power of two and greater than a long */
    public static final int PACKET_SIZE_IN_BYTES = 1024;
    public static final int DEFAULT_NUMBER_OF_CLIENTS = 10;
    public static final long METRICS_REPORT_PERIOD_IN_MILLIS = 1000;
}
