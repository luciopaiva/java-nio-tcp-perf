package com.luciopaiva;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

class Utils {

    private final static long KILO = 1024;
    private final static long MEGA = KILO * KILO;
    private final static long GIGA = KILO * MEGA;

    static String getAddressStr(SocketAddress socketAddress) {
        InetSocketAddress address = (InetSocketAddress) socketAddress;
        return address != null ? address.getHostString() + ":" + address.getPort() : "?";
    }

    static String bytesToStr(long bytes) {
        if (bytes < KILO) {
            return String.valueOf(bytes);
        } else if (bytes < MEGA) {
            return (bytes / KILO) + "k";
        } else {
            return (bytes / MEGA) + "M";
        }
    }
}
