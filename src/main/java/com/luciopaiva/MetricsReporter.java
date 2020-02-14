package com.luciopaiva;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

class MetricsReporter {

    private static final int HEADER_PERIOD_IN_REPORTS = 10;

    private final List<String> fieldNames;
    private final List<Integer> fieldSizes;
    private final List<String> fieldTypes;

    private String metricsHeader;
    private String metricsRow;
    private int countdownToHeader = 0;
    private boolean mustCompileHeader = true;

    MetricsReporter() {
        fieldNames = new ArrayList<>();
        fieldSizes = new ArrayList<>();
        fieldTypes = new ArrayList<>();
    }

    void addField(String name, int size, String format) {
        fieldNames.add(name);
        fieldSizes.add(size);
        fieldTypes.add(format);

        mustCompileHeader = true;
    }

    void report(Object ...args) {
        if (args.length != fieldNames.size()) {
            throw new IllegalArgumentException(String.format("Expected %d arguments, received %d",
                    fieldNames.size(), args.length));
        }

        if (mustCompileHeader) {
            compileHeader();
        }

        if (countdownToHeader == 0) {
            System.out.println(metricsHeader);
            countdownToHeader = HEADER_PERIOD_IN_REPORTS;
        }
        countdownToHeader--;

        System.out.println(String.format(metricsRow, args));
    }

    private void compileHeader() {
        StringJoiner headerJoiner = new StringJoiner("|");
        StringJoiner rowJoiner = new StringJoiner("|");
        int hrSize = 0;
        for (int i = 0; i < fieldNames.size(); i++) {
            headerJoiner.add(String.format(String.format(" %%%ds ", fieldSizes.get(i)), fieldNames.get(i)));
            rowJoiner.add(String.format(" %%%d%s ", fieldSizes.get(i), fieldTypes.get(i)));
            hrSize += fieldSizes.get(i) + 2;  // one leading + one trailing space char
        }
        hrSize += (fieldNames.size() - 1);  // field separators
        String hr = new String(new char[hrSize]).replace('\0', '-');
        metricsHeader = hr + '\n' + headerJoiner.toString() + '\n' + hr;
        metricsRow = rowJoiner.toString();
        mustCompileHeader = false;
    }
}
