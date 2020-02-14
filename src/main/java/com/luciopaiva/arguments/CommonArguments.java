package com.luciopaiva.arguments;

import com.luciopaiva.Constants;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static com.luciopaiva.Constants.SELECT_TIMEOUT_IN_MILLIS;

class CommonArguments {

    final Options options = new Options();

    public boolean debug = false;
    public int port = Constants.SERVER_PORT;
    public int selectTimeoutInMillis = SELECT_TIMEOUT_IN_MILLIS;

    CommonArguments() {
        options.addOption("d", "debug", false, "show debug logs");
        options.addOption("h", "help", false, "show help");
        options.addOption("p", "port", true, "the server port");
        options.addOption("w", "wait", true, "wait time between select()s, in millis");
    }

    private void showHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(" ", this.options);
    }

    static CommandLine parseCommon(CommonArguments arguments, String... args) {
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(arguments.options, args);

            if (cmd.hasOption("h")) {
                arguments.showHelp();
                System.exit(0);
            }

            arguments.debug = cmd.hasOption("d");

            if (cmd.hasOption("p")) {
                arguments.port = Integer.parseInt(cmd.getOptionValue("p"));
            }
            if (cmd.hasOption("w")) {
                arguments.selectTimeoutInMillis = Integer.parseInt(cmd.getOptionValue("w"));
            }

            return cmd;
        } catch (ParseException e) {
            arguments.showHelp();
            System.exit(1);
        }

        return null;
    }
}
