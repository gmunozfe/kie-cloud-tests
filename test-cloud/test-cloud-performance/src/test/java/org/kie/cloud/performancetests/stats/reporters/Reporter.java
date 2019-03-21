package org.kie.cloud.performancetests.stats.reporters;

import java.util.List;

import org.kie.cloud.performancetests.stats.metrics.Measurement;

public interface Reporter {

    /**
     * Reports gathered statistics
     *
     * @param statistics gathered statistics
     */
    public void report(List<Measurement> statistics);
}
