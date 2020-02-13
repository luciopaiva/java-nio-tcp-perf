package com.luciopaiva;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.luciopaiva.Constants.DEFAULT_NUMBER_OF_CLIENTS;

class ClientArguments {

    private static final Options options = new Options();

    static {
        options.addOption("p", "port", true, "the server port");
        options.addOption("h", "host", true, "the server host");
        options.addOption("c", "clients", true, "how many clients to spawn");
    }

    int port = Constants.SERVER_PORT;
    String host = "127.0.0.1";
    int numberOfClients = DEFAULT_NUMBER_OF_CLIENTS;

    static ClientArguments parse(String ...args) {
        CommandLineParser parser = new DefaultParser();
        ClientArguments arguments = new ClientArguments();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("p")) {
                arguments.port = Integer.parseInt(cmd.getOptionValue("p"));
            }
            if (cmd.hasOption("h")) {
                arguments.host = cmd.getOptionValue("h");
            }
            if (cmd.hasOption("c")) {
                arguments.numberOfClients = Integer.parseInt(cmd.getOptionValue("c"));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return arguments;
    }
}
