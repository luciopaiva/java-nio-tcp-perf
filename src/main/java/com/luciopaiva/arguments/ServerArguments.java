package com.luciopaiva.arguments;

import org.apache.commons.cli.CommandLine;

import static com.luciopaiva.Constants.METRICS_REPORT_PERIOD_IN_MILLIS;
import static com.luciopaiva.Constants.PACKET_SIZE_IN_BYTES;
import static com.luciopaiva.Constants.SEND_PERIOD_IN_MILLIS;

public class ServerArguments extends CommonArguments {

    public long metricsPeriodInMillis = METRICS_REPORT_PERIOD_IN_MILLIS;
    public int packetSizeInBytes = PACKET_SIZE_IN_BYTES;
    public long sendPeriodInMillis = SEND_PERIOD_IN_MILLIS;

    private ServerArguments() {
        super();
        options.addOption("e", "send-period", true, "period for sending packets, in millis");
        options.addOption("r", "report-period", true, "period for printing metrics, in millis");
        options.addOption("s", "payload-size", true, "size of the payload to send, in bytes");
    }

    public static ServerArguments parse(String ...args) {
        ServerArguments arguments = new ServerArguments();

        CommandLine cmd = CommonArguments.parseCommon(arguments, args);
        if (cmd == null) {
            System.exit(1);
        }

        if (cmd.hasOption("e")) {
            arguments.sendPeriodInMillis = Long.parseLong(cmd.getOptionValue("e"));
            if (arguments.sendPeriodInMillis < arguments.selectTimeoutInMillis) {
                System.err.println("Send period cannot be less than select() timeout period. Main loop iteration " +
                        "frequency is dictated by that period. To continue, decrease select() timeout period as well.");
                System.exit(1);
            }
        }
        if (cmd.hasOption("r")) {
            arguments.metricsPeriodInMillis = Long.parseLong(cmd.getOptionValue("r"));
            if (arguments.metricsPeriodInMillis < arguments.selectTimeoutInMillis) {
                System.err.println("Metrics period cannot be less than select() timeout period. Main loop iteration " +
                        "frequency is dictated by that period. To continue, decrease select() timeout period as well.");
                System.exit(1);
            }
        }
        if (cmd.hasOption("s")) {
            arguments.packetSizeInBytes = Integer.parseInt(cmd.getOptionValue("s"));
        }

        return arguments;
    }
}
