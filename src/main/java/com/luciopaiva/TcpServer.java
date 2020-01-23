package com.luciopaiva;

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

import static com.luciopaiva.Constants.PACKET_SIZE_IN_BYTES;
import static com.luciopaiva.Constants.SELECT_TIMEOUT;
import static com.luciopaiva.Constants.SERVER_PORT;

public class TcpServer {

    private static final String ADDRESS_IPV4_ANY = "0.0.0.0";
    private static final long METRICS_REPORT_PERIOD_IN_NANOS = 1_000_000_000;
    private static final int HEADER_PERIOD_IN_REPORTS = 10;

    private final int port;
    private final Selector selector;
    private final ServerSocketChannel tcpServerSocketChannel;
    private final HashSet<SocketChannel> clientSocketChannels;
    private final ByteBuffer buffer;
    private final String metricsHeader;

    private boolean isServerActive = true;
    private long nextTimeShouldSend = 0;
    private int countdownToHeader = 0;

    private long successfulSends = 0;
    private long partialSends = 0;
    private long failedSends = 0;

    public TcpServer(int port) throws IOException {
        this.port = port;
        selector = SelectorProvider.provider().openSelector();

        metricsHeader = " good    | partial | failed   ";

        // prepare buffer with random data to send
        Random random = new Random(42);
        buffer = ByteBuffer.allocate(PACKET_SIZE_IN_BYTES);
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
        this.tcpServerSocketChannel.bind(new InetSocketAddress(host, port));

        System.out.println(String.format("Server started at %s. Entering main loop...",
                Utils.getAddressStr(tcpServerSocketChannel.getLocalAddress())));

        while (isServerActive) {
            try {
                if (selector.select(SELECT_TIMEOUT) > 0) {
                    selector.selectedKeys().forEach(this::handleSelectionKey);
                    selector.selectedKeys().clear();
                }

                long now = System.nanoTime();
                if (nextTimeShouldSend <= now) {
                    sendDataToAllClients();
                    reportMetrics();
                    nextTimeShouldSend = now + METRICS_REPORT_PERIOD_IN_NANOS;
                }

            } catch (ClosedSelectorException e) {
                isServerActive = false;
                System.out.println("Selector was closed. Terminating...");
            }
        }
    }

    private void sendDataToAllClients() {
        successfulSends = 0;
        partialSends = 0;
        failedSends = 0;

        for (SocketChannel client : clientSocketChannels) {
            try {
                long written = client.write(buffer);
                if (written == PACKET_SIZE_IN_BYTES) {
                    successfulSends++;
                } else if (written == 0) {
                    failedSends++;
                } else {
                    partialSends++;
                }
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
        System.out.println(String.format(" %7d | %7d | %7d ", successfulSends, partialSends, failedSends));
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
        int port = SERVER_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        TcpServer server = new TcpServer(port);
        server.run();
    }
}
