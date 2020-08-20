
# Java TCP load and performance test

![](screenshots/screenshot-20200124-0200.png)

This is an experiment to test some load and performance aspects of a Java NIO TCP server.

It is basically made of two parts: TcpServer and TcpClients. The former runs a vanilla NIO non-blocking server and the latter spawns a certain number of NIO, also non-blocking, TCP clients which will connect to the server part.

Upon connection, the client doesn't send anything. It just sits waiting for data from the server. The server, on its turn, will start sending random bytes of data to each connected client.

## How to build and run

Note: these instructions are meant for Linux, but can be easily adapted to other systems.

Make sure you have at least Java 1.8. To install it:

    sudo yum install java-1.8.0-openjdk

Then use this to change the system's current Java to the newly installed:

    sudo update-alternatives --config java

On the machine you'll build the project, make sure you have Gradle installed. To build it:

    gradle build
    
Then run both `server.sh` and `client.sh`, each on a separate terminal window. They will look for the jar file, which should be either in the same folder or in `build/libs`.

This will run the server on port 3000 (the default port):

    ./server.sh

And then you can start the clients on the same machine for a simple test:

    ./client.sh

This will spawn 10 clients by default.

## Advanced usage

Both client and server can accept several arguments.

### Common arguments

These are arguments valid for both client and server:

* `-h,--help`: show a brief help message with all arguments available;
* `-p,--port <PORT>`: which port the server should be listening at (default: 3000);
* `-d,--debug`: show some verbose logs;
* `-r,--report-period <PERIOD>`: period for printing metrics, in millis (default: 1000 ms);
* `-w,--wait <TIME>`: the main loop is governed by a while loop that sleeps a bit every iteration so that it doesn't draw too much CPU power. That sleep is triggered by a `select()` call and that call will, by default, wait at most 200 ms before returning to the loop flow. This wait time can be adjusted by this argument;

### Server-side arguments

* `-s,--payload-size <SIZE>`: how many bytes to send each client (default: 1024 bytes);
* `-e,--send-period <PERIOD>`: this determines how frequently to send the payload to each client. By default, it sends `--payload-size` bytes to each client every 200 ms;
* `-g,--send-strategy <STRATEGY>`: this dictates how the server sends packets to clients. Two strategies exist:
  - `burst`: this is the default. At the beginning of every `--send-period` window, the server will send to all clients "at once". Since the server runs a single thread, of course, it takes some time to copy all the data to each socket buffers, so it doesn't happen instantaneously. However, it will hopefully finish copying before the end of the period, so it can keep up with the requested sending rate;
  - `uniform`: this is an experimental approach the spreads sends across the given send period. The send period is divided into buckets (a hundred, currently hardcoded) and each new client that connects gets randomly assigned to one of these buckets, each bucket potentially holding multiple clients. Right now it doesn't perform as efficiently as the `burst` strategy due to the additional logic required, but it can possibly be improved.

### Client-side arguments

* `-a,--address <ADDRESS>`: the server host address. To specify a port, use the `-p` argument;
* `-c,--clients <NUMBER>`: how many clients to spawn (default: 10).

## Things learned

### Do not create sockets in a separate thread in Java NIO

For some reason when developing this test, I thought it would be wise to create my client connections in a separate thread, register the associated socket channels for OP_CONNECT and then handle them in the main thread, after they were allegedly ready for `finishConnect()`.

This did not work as the socket channel object is not thread safe, thus not guaranteed to be synchronized. I was frequently getting a `NoConnectionPendingException` calling `finishConnect()` when running tests because apparently the main thread saw SelectionKey firing the `OP_CONNECT` event, but the SocketChannel was not fresh in main thread's local memory, so `finishConnect()` was failing because it thought `connect()` was not called yet (some internal variable was probably old in main thread's point of view).

Moreover, establishing a new connection in a separate thread takes an absurd amount of time. The application freezes for seconds. Not sure what happens there, but anyway... fixed by moving everything to the main thread (it is non-blocking, after all).

### Useful commands

To prevent TCP ACKs from getting to the server, run this on the client machine:

    sudo iptables -A OUTPUT -p tcp --dport 3000 -j DROP

To list existing firewall rules:

    sudo iptables -S

And to list with rule number:

    sudo iptables -L -v -n --line-numbers

To delete a rule (e.g., rule 1):

    sudo iptables -D OUTPUT 1

Run this to observe client sockets and their queues:

    watch -n 1 'cat /proc/net/tcp | grep 0BD0'

Or, better yet, use [tcptop](https://github.com/luciopaiva/tcptop) for that.

### Relevant read

https://en.wikipedia.org/wiki/Software_performance_testing
