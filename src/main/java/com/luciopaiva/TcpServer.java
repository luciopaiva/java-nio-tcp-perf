package com.luciopaiva;

import com.luciopaiva.arguments.ServerArguments;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Random;

public class TcpServer {

    private static final String ADDRESS_IPV4_ANY = "0.0.0.0";
    private static final int HEADER_PERIOD_IN_REPORTS = 10;

    private final Selector selector;
    private final ServerSocketChannel tcpServerSocketChannel;
    private final HashSet<SocketChannel> clientSocketChannels;
    private final ByteBuffer buffer;
    private final String metricsHeader;
    private final ServerArguments arguments;
    private final long metricsReportPeriodInNanos;
    private final long sendPeriodPeriodInNanos;

    private boolean isServerActive = true;
    private long nextTimeShouldSend = 0;
    private long nextTimeShouldReportMetrics = 0;
    private int countdownToHeader = 0;

    /* metrics */
    private long successfulSends = 0;
    private long partialSends = 0;
    private long failedSends = 0;
    private double loadFactorSum = 0;
    private double loadFactorCount = 0;
    private double maxLoadFactor = 0;
    private long bytesSent = 0;

    private TcpServer(ServerArguments arguments) throws IOException {
        this.arguments = arguments;
        metricsReportPeriodInNanos = arguments.metricsPeriodInMillis * 1_000_000;
        sendPeriodPeriodInNanos = arguments.sendPeriodInMillis * 1_000_000;

        selector = SelectorProvider.provider().openSelector();

        metricsHeader = String.format(" %7s | %7s | %7s | %7s | %7s | %7s", "LF",
                "max(LF)", "good", "partial", "failed", "out");

        // prepare buffer with random data to send
        Random random = new Random(42);
        buffer = ByteBuffer.allocate(arguments.packetSizeInBytes);
        while (buffer.hasRemaining()) {
            buffer.putLong(random.nextLong());
        }
        buffer.flip();

        clientSocketChannels = new HashSet<>();

        tcpServerSocketChannel = ServerSocketChannel.open();
        tcpServerSocketChannel.configureBlocking(false);

        tcpServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void run() throws IOException {
        InetAddress host = InetAddress.getByName(ADDRESS_IPV4_ANY);
        this.tcpServerSocketChannel.bind(new InetSocketAddress(host, arguments.port));

        System.out.println(String.format("Server started at %s. Entering main loop...",
                Utils.getAddressStr(tcpServerSocketChannel.getLocalAddress())));

        while (isServerActive) {
            try {
                if (selector.select(arguments.selectTimeoutInMillis) > 0) {
                    selector.selectedKeys().forEach(this::handleSelectionKey);
                    selector.selectedKeys().clear();
                }

                long now = System.nanoTime();

                if (nextTimeShouldSend <= now) {
                    sendDataToAllClients();
                    nextTimeShouldSend = now + sendPeriodPeriodInNanos;

                    // update load factor metrics
                    long elapsed = System.nanoTime() - now;
                    double loadFactor = elapsed / (double) sendPeriodPeriodInNanos;
                    loadFactorSum += loadFactor;
                    loadFactorCount++;
                    if (loadFactor > maxLoadFactor) {
                        maxLoadFactor = loadFactor;
                    }
                }

                if (nextTimeShouldReportMetrics <= now) {
                    reportMetrics();
                    nextTimeShouldReportMetrics = now + metricsReportPeriodInNanos;
                }

            } catch (ClosedSelectorException e) {
                isServerActive = false;
                System.out.println("Selector was closed. Terminating...");
            }
        }
    }

    private void sendDataToAllClients() {
        for (SocketChannel client : clientSocketChannels) {
            try {
                long written = client.write(buffer);
                if (written == arguments.packetSizeInBytes) {
                    successfulSends++;
                } else if (written == 0) {
                    failedSends++;
                } else {
                    partialSends++;
                }
                bytesSent += written;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                buffer.position(0);  // no matter how much we've read, move the pointer back to the start
            }
        }
    }

    private void reportMetrics() {
        if (countdownToHeader == 0) {
            System.out.println(metricsHeader);
            countdownToHeader = HEADER_PERIOD_IN_REPORTS;
        }
        countdownToHeader--;
        int loadFactor = (int) (100 * (loadFactorSum / loadFactorCount));
        int maxLoadFactor = (int) (100 * this.maxLoadFactor);
        System.out.println(String.format(" %7d | %7d | %7d | %7d | %7d | %7s ",
                loadFactor, maxLoadFactor, successfulSends, partialSends, failedSends, Utils.bytesToStr(bytesSent)));

        resetMetrics();
    }

    private void resetMetrics() {
        successfulSends = 0;
        partialSends = 0;
        failedSends = 0;
        loadFactorSum = 0;
        loadFactorCount = 0;
        maxLoadFactor = 0;
        bytesSent = 0;
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
                    throw new Error("Unknown SelectableChannel type '" + selectableChannel.getClass().getName() + "'.");
                }
            } else {
                closeKey(selectionKey);
            }
        } catch (CancelledKeyException ignored) {
            // key was cancelled, no big deal; just move on
        } catch (IOException e) {
            System.err.println("Something wrong happened with this key.");
        }
    }

    private void readFromKey(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        // ToDo remove this buffer creation from here
        ByteBuffer buffer = ByteBuffer.allocate(64);
        int read = socketChannel.read(buffer);
        if (read < 0) {
            System.out.println("Nothing to read, socket probably already closed.");
            closeKey(selectionKey);
        } else if (read == 0) {
            System.out.println("Received read notification but there was nothing there.");
        } else {
            System.out.println(String.format("Read %d bytes from client socket.", read));
        }
    }

    private void acceptNewTcpConnection() throws IOException {
        SocketChannel socketChannel = tcpServerSocketChannel.accept();
        // this is probably not necessary because the server socket was already set to non-blocking and
        // I don't think we can change the configuration after accept() is called anyway
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        int sendBufferLength = socketChannel.getOption(StandardSocketOptions.SO_SNDBUF);
        int recvBufferLength = socketChannel.getOption(StandardSocketOptions.SO_RCVBUF);

        clientSocketChannels.add(socketChannel);

        System.out.println(String.format("Connection accepted (sndbuf: %d, recvbuf: %d).", sendBufferLength, recvBufferLength));
    }

    private void closeKey(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            socketChannel.close();
        } catch (IOException ignored) {
        } finally {
            clientSocketChannels.remove(socketChannel);
            selectionKey.cancel();
        }
    }

    public static void main(String ...args) throws IOException {
        ServerArguments arguments = ServerArguments.parse(args);

        TcpServer server = new TcpServer(arguments);
        server.run();
    }
}
