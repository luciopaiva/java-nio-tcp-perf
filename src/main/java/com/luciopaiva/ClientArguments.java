package com.luciopaiva;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.luciopaiva.Constants.DEFAULT_NUMBER_OF_CLIENTS;
import static com.luciopaiva.Constants.SELECT_TIMEOUT_IN_MILLIS;

class ClientArguments {

    private static final Options options = new Options();

    static {
        options.addOption("h", "help", false, "show help");
        options.addOption("p", "port", true, "the server port");
        options.addOption("a", "address", true, "the server host");
        options.addOption("c", "clients", true, "how many clients to spawn");
        options.addOption("w", "wait", true, "wait time between select()s, in millis");
    }

    int port = Constants.SERVER_PORT;
    String host = "127.0.0.1";
    int numberOfClients = DEFAULT_NUMBER_OF_CLIENTS;
    int selectTimeoutInMillis = SELECT_TIMEOUT_IN_MILLIS;

    static ClientArguments parse(String ...args) {
        CommandLineParser parser = new DefaultParser();
        ClientArguments arguments = new ClientArguments();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(" ", options);
                System.exit(0);
            }

            if (cmd.hasOption("p")) {
                arguments.port = Integer.parseInt(cmd.getOptionValue("p"));
            }
            if (cmd.hasOption("a")) {
                arguments.host = cmd.getOptionValue("a");
            }
            if (cmd.hasOption("c")) {
                arguments.numberOfClients = Integer.parseInt(cmd.getOptionValue("c"));
            }
            if (cmd.hasOption("w")) {
                arguments.selectTimeoutInMillis = Integer.parseInt(cmd.getOptionValue("w"));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return arguments;
    }
}
