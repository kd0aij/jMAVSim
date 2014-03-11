/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package me.drton.jmavsim;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author markw
 */
public class PerfCounterNano {

    private final Logger logger;
    private final String eventName;
    private long lastReport;
    private long baseTime;
    private int eventCount;
    private double rate;

    // reporting interval in nanoseconds
    private final long rptInterval;

    // all times in nanoseconds
    private long interval_start_t;
    private long last_t;
    private double avg_interval;

    private long min_interval = Long.MAX_VALUE;
    private long max_interval;
    double firstInterval;
    double binScale;

    // histogram range is from min_interval to max_interval
    private int NUM_BINS;
    private int[] histogram;
    private long hist_min;
    private long hist_max;

    public PerfCounterNano(Logger logger, String eventName, int num_bins,
            long hist_min, long hist_max, long rptInterval) {
        super();
        long t = System.nanoTime();
        baseTime = t;
        interval_start_t = t;
        this.logger = logger;
        this.eventName = eventName;
        this.NUM_BINS = num_bins;
        histogram = new int[NUM_BINS];
        this.hist_min = hist_min;
        this.hist_max = hist_max;
        this.rptInterval = rptInterval;
        lastReport = baseTime;
        binScale = (double) (NUM_BINS - 1) / (hist_max - hist_min);
        firstInterval = hist_min;
    }

    // event time in nanoseconds
    public void event(long t) {
        eventCount++;
        long et = t - interval_start_t;
        long dt = t - last_t;
        min_interval = Math.min(dt, min_interval);
        max_interval = Math.max(dt, max_interval);
        last_t = t;

        int binIndex;
        binIndex = (int) ((dt - firstInterval) * binScale);
        binIndex = Math.max(0, binIndex);
        binIndex = Math.min((NUM_BINS - 1), binIndex);
        histogram[binIndex]++;

        // generate log entry
        if (et >= rptInterval) {
            avg_interval = et / eventCount;
            rate = 1e9 / avg_interval; // Hz

            logger.log(Level.INFO,
                    String.format("%10.3f, %s rate: %5.1f, dt(msec) avg: %5.1f, min: %5.1f, max: %5.1f",
                            1e-9 * (t - baseTime), eventName, rate, 1e-6 * avg_interval,
                            1e-6 * min_interval, 1e-6 * max_interval));
            StringBuilder hist = new StringBuilder();
            double invScale = (hist_max - hist_min) / (NUM_BINS - 1);
            firstInterval = hist_min;
            for (int index = 0; index < NUM_BINS; index++) {
                double interval = firstInterval + (index * invScale);
                hist.append(String.format("%5.2f: %d\n", 1e-6 * interval, histogram[index]));
            }
            logger.log(Level.INFO, hist.toString());

            interval_start_t = t;
            eventCount = 0;
            min_interval = Long.MAX_VALUE;
            max_interval = 0;
            for (int index = 0; index < NUM_BINS; index++) {
                histogram[index] = 0;
            }
        }
    }
}
