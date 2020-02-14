package com.luciopaiva;

import com.luciopaiva.arguments.ClientArguments;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

@SuppressWarnings("FieldCanBeLocal")
public class TcpClients {

    private final MetricsReporter reporter;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final ClientArguments arguments;
    private final ByteBuffer receiveBuffer;
    private final long metricsReportPeriodInNanos;

    private int activeKeys;
    private long numberOfSocketsToConnect;
    private long nextTimeShouldReportMetrics;

    private long bytesReceived;
    private long connectedClients;
    private long connectionSucceeded;
    private long connectionFailed;

    private TcpClients(ClientArguments arguments) throws IOException {
        this.arguments = arguments;

        metricsReportPeriodInNanos = arguments.metricsPeriodInMillis * 1_000_000;

        numberOfSocketsToConnect = arguments.numberOfClients;

        reporter = new MetricsReporter();
        reporter.addField("clients", 7, "d");
        reporter.addField("waiting", 7, "d");
        reporter.addField("connect", 7, "d");
        reporter.addField("confail", 7, "d");
        reporter.addField("in", 7, "s");

        selector = Selector.open();
        serverAddress = new InetSocketAddress(arguments.host, arguments.port);
        activeKeys = arguments.numberOfClients;
        receiveBuffer = ByteBuffer.allocate(Constants.PACKET_SIZE_IN_BYTES);
    }

    private void run() {
        try {
            while (activeKeys > 0) {
                if (numberOfSocketsToConnect > 0) {
                    createConnections();
                }

                if (selector.select(arguments.selectTimeoutInMillis) > 0) {
                    for (SelectionKey selectionKey : selector.selectedKeys()) {
                        handleSelectionKey(selectionKey);
                    }
                    selector.selectedKeys().clear();
                }

                long now = System.nanoTime();

                if (nextTimeShouldReportMetrics <= now) {
                    reportMetrics();
                    nextTimeShouldReportMetrics = now + metricsReportPeriodInNanos;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("No more active keys. Terminating...");
    }

    private void reportMetrics() {
        reporter.report(connectedClients, numberOfSocketsToConnect, connectionSucceeded, connectionFailed,
                Utils.bytesToStr(bytesReceived));
        resetMetrics();
    }

    private void resetMetrics() {
        connectionSucceeded = 0;
        connectionFailed = 0;
        bytesReceived = 0;
    }

    private void handleSelectionKey(SelectionKey selectionKey) {
        if (!selectionKey.isValid()) {
            closeKey(selectionKey);
        } else if (selectionKey.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            try {
                if (socketChannel.finishConnect()) {
                    if (arguments.debug) {
                        System.out.println("Connected.");
                    }
                    // unregister for OP_CONNECT (important otherwise select() will return immediately),
                    // register for OP_READ
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    connectionSucceeded++;
                    connectedClients++;
                } else {
                    System.err.println("Error establishing socket connection.");
                    connectionFailed++;
                    numberOfSocketsToConnect++;  // let's keep trying
                }
            } catch (IOException e) {
                if (arguments.debug) {
                    System.err.println("Connection failed: " + e.getMessage());
                }
                activeKeys--;
                connectionFailed++;
                numberOfSocketsToConnect++;  // let's keep trying
            }
        } else if (selectionKey.isReadable()) {
            try {
                readFromKey(selectionKey);
            } catch (IOException e) {
                if (arguments.debug) {
                    System.err.println("Error reading from key. Proceeding to close it...");
                }
                closeKey(selectionKey);
            }
        } else {
            System.out.println("wat");
        }
    }

    private void readFromKey(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        while (true) {
            receiveBuffer.clear();
            int read = socketChannel.read(receiveBuffer);
            if (read < 0) {
                if (arguments.debug) {
                    System.out.println("Nothing to read, socket probably already closed");
                }
                closeKey(selectionKey);
                break;
            } else if (read == 0) {
                break;  // nothing else to read
            } else {
                bytesReceived += read;
            }
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
            connectedClients--;
        }
        if (arguments.debug) {
            System.out.println("Key closed. Keys still active: " + activeKeys);
        }
    }

    private void createConnections() throws IOException {
        for (int i = 0; i < numberOfSocketsToConnect; i++) {
            createSocketChannel();
            numberOfSocketsToConnect--;
        }
    }

    private void createSocketChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        int sendBufferLength = socketChannel.getOption(StandardSocketOptions.SO_SNDBUF);
        int recvBufferLength = socketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
        if (arguments.debug) {
            System.out.println(String.format("Creating new socket (sndbuf: %d, recvbuf: %d)...",
                    sendBufferLength, recvBufferLength));
        }
        socketChannel.connect(serverAddress);
    }

    public static void main(String ...args) throws IOException {
        System.out.println("Started!");

        ClientArguments arguments = ClientArguments.parse(args);

        TcpClients clients = new TcpClients(arguments);
        clients.run();
    }
}
