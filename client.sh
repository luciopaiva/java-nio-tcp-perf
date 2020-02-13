
BUILD_DIR=build/libs

# if the directory exists, we're running locally; otherwise, trust the jar is in the same folder
if [ -d "$BUILD_DIR" ]; then
  cd $BUILD_DIR || exit
fi

java -Djava.net.preferIPv4Stack=true -cp tcp-java-perf-1.0-SNAPSHOT.jar com.luciopaiva.TcpClientBatch "$@"
