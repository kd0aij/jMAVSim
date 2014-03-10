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

    // histogram range is from min_interval to max_interval
    private int NUM_BINS = 21;
    private int[] histogram = new int[NUM_BINS];
    private long hist_min = 0;
    private long hist_max = 0;

    public PerfCounterNano(Logger logger, String eventName, long rptInterval) {
        super();
        long t = System.nanoTime();
        baseTime = t;
        interval_start_t = t;
        this.logger = logger;
        this.eventName = eventName;
        this.rptInterval = rptInterval;
        lastReport = baseTime;
    }

    // seconds
    public void setHist_min(double hist_min) {
        this.hist_min = (long)(1e9 * hist_min);
    }

    // seconds
    public void setHist_max(double hist_max) {
        this.hist_max = (long)(1e9 * hist_max);
    }

    public void setNUM_BINS(int NUM_BINS) {
        this.NUM_BINS = NUM_BINS;
    }

    // event time in nanoseconds
    public void event(long t) {
        eventCount++;
        long et = t - interval_start_t;
        long dt = t - last_t;
        min_interval = Math.min(dt, min_interval);
        max_interval = Math.max(dt, max_interval);
        last_t = t;

        double binScale;
        int binIndex;
        if ((hist_min == 0) && (hist_max == 0) && (max_interval - min_interval > 0)) {
            // autoscale histogram
            binScale = (double) NUM_BINS / (max_interval - min_interval);
            binIndex = (int) ((dt - min_interval) * binScale);
        } else {
            binScale = (double) NUM_BINS / (hist_max - hist_min);
            binIndex = (int) ((dt - min_interval) * binScale);
        }
        binIndex = Math.max(0, binIndex);
        binIndex = Math.min((NUM_BINS - 1), binIndex);
        histogram[binIndex]++;

        // generate log entry
        if ((t - interval_start_t) >= rptInterval) {
            avg_interval = et / eventCount;
            rate = 1e9 / avg_interval; // Hz

            logger.log(Level.INFO,
                    String.format("%10.3f, %s rate: %5.1f, dt(msec) avg: %5.1f, min: %5.1f, max: %5.1f",
                            1e-9 * (t - baseTime), eventName, rate, 1e-6 * avg_interval,
                            1e-6 * min_interval, 1e-6 * max_interval));
            StringBuilder hist = new StringBuilder();
            double firstInterval;
            if ((hist_min == 0) && (hist_max == 0)) {
                binScale = (max_interval - min_interval) / (NUM_BINS - 1);
                firstInterval = min_interval;
            } else {
                binScale = (hist_max - hist_min) / (NUM_BINS - 1);
                firstInterval = hist_min;
            }
            for (int index = 0; index < NUM_BINS; index++) {
                double interval = firstInterval + (index * binScale);
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
