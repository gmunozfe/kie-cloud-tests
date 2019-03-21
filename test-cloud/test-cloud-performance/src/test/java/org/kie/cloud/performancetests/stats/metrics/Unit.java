package org.kie.cloud.performancetests.stats.metrics;


public enum Unit {
    SECOND("s"),
    NONE("");

    private String stringRepresentation;

    Unit(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    public String getStringRepresentation() {
        return stringRepresentation;
    }
}
