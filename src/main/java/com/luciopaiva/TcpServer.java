package com.luciopaiva;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import static com.luciopaiva.Constants.SELECT_TIMEOUT;
import static com.luciopaiva.Constants.SERVER_PORT;

public class TcpServer {

    private static final String ADDRESS_IPV4_ANY = "0.0.0.0";

    private final Selector selector;
    private final ServerSocketChannel tcpServerSocketChannel;

    private boolean isServerActive = true;

    public TcpServer() throws IOException {
        selector = SelectorProvider.provider().openSelector();

        tcpServerSocketChannel = ServerSocketChannel.open();
        tcpServerSocketChannel.configureBlocking(false);

        tcpServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void run() throws IOException {
        InetAddress host = InetAddress.getByName(ADDRESS_IPV4_ANY);
        this.tcpServerSocketChannel.bind(new InetSocketAddress(host, SERVER_PORT));

        System.out.println(String.format("Server started at %s. Entering main loop...",
                Utils.getAddressStr(tcpServerSocketChannel.getLocalAddress())));

        while (isServerActive) {
            try {
                if (selector.select(SELECT_TIMEOUT) > 0) {
                    selector.selectedKeys().forEach(this::handleSelectionKey);
                    selector.selectedKeys().clear();
                }
            } catch (ClosedSelectorException e) {
                isServerActive = false;
                System.out.println("Selector was closed. Terminating...");
            }
        }
    }

    private void handleSelectionKey(SelectionKey selectionKey) {
        try {
            if (selectionKey.isValid()) {
                SelectableChannel selectableChannel = selectionKey.channel();
                if (selectableChannel instanceof ServerSocketChannel && selectionKey.isAcceptable()) {
                    acceptNewTcpConnection();
                } else if (selectableChannel instanceof SocketChannel && selectionKey.isReadable()) {
                    readFromKey(selectionKey);
                } else {
                    throw new Error("Unknown SelectableChannel type '" + selectableChannel.getClass().getName() + "'");
                }
            } else {
                closeKey(selectionKey);
            }
        } catch (CancelledKeyException ignored) {
            // key was cancelled, no big deal; just move on
        } catch (IOException e) {
            System.err.println("Something wrong happened with this key");
        }
    }

    private void readFromKey(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        // ToDo remove this buffer creation from here
        ByteBuffer buffer = ByteBuffer.allocate(64);
        int read = socketChannel.read(buffer);
        if (read < 0) {
            System.out.println("Nothing to read, socket probably already closed");
            closeKey(selectionKey);
        }
    }

    private void acceptNewTcpConnection() throws IOException {
        SocketChannel socketChannel = tcpServerSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        System.out.println("Connection accepted");
    }

    private void closeKey(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            socketChannel.close();
        } catch (IOException ignored) {
        } finally {
            selectionKey.cancel();
        }
    }

    public static void main(String ...args) throws IOException {
        TcpServer server = new TcpServer();
        server.run();
    }
}
