package com.luciopaiva;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static com.luciopaiva.Constants.SELECT_TIMEOUT;

@SuppressWarnings("FieldCanBeLocal")
public class TcpClientBatch {

    private final int NUMBER_OF_CLIENTS = 10;

    private final Selector selector;
    private final InetSocketAddress serverAddress;

    private int activeKeys = NUMBER_OF_CLIENTS;

    public TcpClientBatch(String host, int port) throws IOException {
        selector = Selector.open();
        serverAddress = new InetSocketAddress(host, port);
    }

    public void run() throws InterruptedException {
        Thread connectionCreatorThread = new Thread(this::createConnectionBatch);
        connectionCreatorThread.start();

        try {
            while (activeKeys > 0) {
                if (selector.select(SELECT_TIMEOUT) > 0) {
                    for (SelectionKey selectionKey : selector.selectedKeys()) {
                        handleSelectionKey(selectionKey);
                    }
                    selector.selectedKeys().clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("No more active keys. Terminating...");

        connectionCreatorThread.join();
    }

    private void handleSelectionKey(SelectionKey selectionKey) {
        if (!selectionKey.isValid()) {
            closeKey(selectionKey);
        } else if (selectionKey.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            try {
                if (socketChannel.finishConnect()) {
                    System.out.println("Connected.");
                    // unregister for OP_CONNECT (important otherwise select() will return immediately),
                    // register for OP_READ
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else {
                    System.err.println("Error establishing socket connection.");
                }
            } catch (IOException e) {
                System.err.println("Connection failed: " + e.getMessage());
                activeKeys--;
            }
        } else if (selectionKey.isReadable()) {
            try {
                readFromKey(selectionKey);
            } catch (IOException e) {
                System.err.println("Error reading from key. Proceeding to close it...");
                closeKey(selectionKey);
            }
        } else {
            System.out.println("wat");
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

    private void closeKey(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            socketChannel.close();
        } catch (IOException ignored) {
        } finally {
            selectionKey.cancel();
            activeKeys--;
        }
        System.out.println("Key closed. Keys still active: " + activeKeys);
    }

    private void createConnectionBatch() {
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            try {
                createSocketChannel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createSocketChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        int sendBufferLength = socketChannel.getOption(StandardSocketOptions.SO_SNDBUF);
        int recvBufferLength = socketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
        System.out.println(String.format("Creating new socket (sndbuf: %d, recvbuf: %d)...", sendBufferLength, recvBufferLength));
        socketChannel.connect(serverAddress);
    }

    public static void main(String ...args) throws IOException, InterruptedException {
        System.out.println("Started!");

        if (args.length < 1) {
            System.err.println("Params expected:");
            System.err.println("- host");
            System.err.println("- [port]");
            System.exit(1);
        }

        String host = args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Constants.SERVER_PORT;

        TcpClientBatch clients = new TcpClientBatch(host, port);
        clients.run();
    }
}
