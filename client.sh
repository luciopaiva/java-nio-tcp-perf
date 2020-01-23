
cd build/libs

java -Djava.net.preferIPv4Stack=true -cp tcp-java-perf-1.0-SNAPSHOT.jar com.luciopaiva.TcpClientBatch 127.0.0.1 3023
