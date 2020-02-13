package com.luciopaiva;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class ServerArguments {

    private static final Options options = new Options();

    static {
        options.addOption("p", "port", true, "the server port");
    }

    int port = Constants.SERVER_PORT;

    static ServerArguments parse(String ...args) {
        CommandLineParser parser = new DefaultParser();
        ServerArguments arguments = new ServerArguments();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("p")) {
                arguments.port = Integer.parseInt(cmd.getOptionValue("p"));
            }
        } catch (ParseException ignored) {}

        return arguments;
    }
}
