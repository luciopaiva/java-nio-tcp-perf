package com.luciopaiva;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Utils {

    static String getAddressStr(SocketAddress socketAddress) {
        InetSocketAddress address = (InetSocketAddress) socketAddress;
        return address != null ? address.getHostString() + ":" + address.getPort() : "?";
    }
}
