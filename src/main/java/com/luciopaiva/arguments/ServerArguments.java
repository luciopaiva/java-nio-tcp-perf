package com.luciopaiva.arguments;

import org.apache.commons.cli.CommandLine;

import static com.luciopaiva.Constants.METRICS_REPORT_PERIOD_IN_MILLIS;

public class ServerArguments extends CommonArguments {

    public long metricsPeriodInMillis = METRICS_REPORT_PERIOD_IN_MILLIS;

    private ServerArguments() {
        options.addOption("r", "report-period", true, "period for printing metrics, in millis");
    }

    public static ServerArguments parse(String ...args) {
        ServerArguments arguments = new ServerArguments();

        CommandLine cmd = CommonArguments.parseCommon(arguments, args);
        if (cmd == null) {
            System.exit(1);
        }

        if (cmd.hasOption("r")) {
            arguments.metricsPeriodInMillis = Long.parseLong(cmd.getOptionValue("r"));
        }

        return arguments;
    }
}
