
# Java TCP performance test

This is an experiment to test some performance aspects of a Java TCP server.

It is basically made of two parts: TcpServer and TcpClientBatch. The former runs a vanilla NIO non-blocking server and the latter spawns a certain number of NIO, also non-blocking, TCP clients which will connect to the server part.

Upon connection, the client doesn't send anything. It just sits waiting for data from the server. The server, on its turn, will start sending random bytes of data to each connected client.

## How to run

To run it, don't forget to pass the `-Djava.net.preferIPv4Stack=true` parameter if you want to stick to IPv4.
