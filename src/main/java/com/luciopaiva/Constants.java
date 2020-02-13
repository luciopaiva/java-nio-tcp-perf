package com.luciopaiva;

class Constants {

    /** How long to let the process sleep while waiting for a select() */
    static final int SELECT_TIMEOUT_IN_MILLIS = 100;
    static final int SERVER_PORT = 3023;
    /** must be a power of two and greater than a long */
    static final int PACKET_SIZE_IN_BYTES = 1024;
    static final int DEFAULT_NUMBER_OF_CLIENTS = 10;
}
