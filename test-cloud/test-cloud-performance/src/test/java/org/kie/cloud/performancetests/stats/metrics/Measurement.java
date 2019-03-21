package org.kie.cloud.performancetests.stats.metrics;


public class Measurement {

    private String name;

    private String value;

    private Metric metric;

    private Unit unit;

    public Measurement(String name, String value, Metric metric, Unit unit) {
        this.name = name;
        this.value = value;
        this.metric = metric;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}
