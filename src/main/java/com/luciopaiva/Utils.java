package com.luciopaiva;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

class Utils {

    private final static long KILO = 1024;
    private final static long HUNDRED_KILO = 100 * KILO;
    private final static long MEGA = KILO * KILO;
    private final static long HUNDRED_MEGA = 100 * MEGA;

    static String getAddressStr(SocketAddress socketAddress) {
        InetSocketAddress address = (InetSocketAddress) socketAddress;
        return address != null ? address.getHostString() + ":" + address.getPort() : "?";
    }

    static String bytesToStr(long bytes) {
        if (bytes < HUNDRED_KILO) {
            return String.valueOf(bytes);
        } else if (bytes < HUNDRED_MEGA) {
            return (bytes / KILO) + "k";
        } else {
            return (bytes / MEGA) + "M";
        }
    }
}
