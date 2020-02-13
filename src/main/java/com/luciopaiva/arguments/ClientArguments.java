package com.luciopaiva.arguments;

import org.apache.commons.cli.CommandLine;

import static com.luciopaiva.Constants.DEFAULT_NUMBER_OF_CLIENTS;

public class ClientArguments extends CommonArguments {

    public String host = "127.0.0.1";
    public int numberOfClients = DEFAULT_NUMBER_OF_CLIENTS;

    private ClientArguments() {
        super();
        options.addOption("a", "address", true, "the server host");
        options.addOption("c", "clients", true, "how many clients to spawn");
    }

    public static ClientArguments parse(String ...args) {
        ClientArguments arguments = new ClientArguments();

        CommandLine cmd = CommonArguments.parseCommon(arguments, args);
        if (cmd == null) {
            System.exit(1);
        }

        if (cmd.hasOption("a")) {
            arguments.host = cmd.getOptionValue("a");
        }
        if (cmd.hasOption("c")) {
            arguments.numberOfClients = Integer.parseInt(cmd.getOptionValue("c"));
        }

        return arguments;
    }
}
