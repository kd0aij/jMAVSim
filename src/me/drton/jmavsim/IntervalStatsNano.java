/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.drton.jmavsim;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author markw
 */
public class IntervalStatsNano {

    private final Logger logger;
    private final String eventName;
    private DescriptiveStatistics stats;
    private long baseTime;
    private long rptInterval;
    private double rate;

    // all times in nanoseconds
    private long interval_start_t;
    private long last_t;

    public IntervalStatsNano(Logger logger, String eventName, 
            int num_samples, long rptInterval) {
        this.logger = logger;
        this.eventName = eventName;
        this.stats = new DescriptiveStatistics(num_samples);
        this.baseTime = 0;
        this.rptInterval = rptInterval;
    }

    // event time in nanoseconds
    public void event(long t) {
        if (baseTime == 0) {
            baseTime = t;
            interval_start_t = t;
            last_t = t;
            return;
        }
        // convert to units of milliseconds
        double dt = 1e-6 * (double)(t - last_t);
        stats.addValue(dt);
        last_t = t;
        
        // generate log entry
        long et = t - interval_start_t;
        if (et >= rptInterval) {
            interval_start_t = t;
            logger.log(Level.INFO,
                    String.format("t: %6.3f, %s %d: dt(msec) avg: %5.1f, min: %5.1f, max: %5.1f, stdev: %5.1f",
                            1e-9 * (t - baseTime), eventName, stats.getN(), stats.getMean(),
                            stats.getMin(), stats.getMax(), stats.getStandardDeviation()));
        }
    }
}
