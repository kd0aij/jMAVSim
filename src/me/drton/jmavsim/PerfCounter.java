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
public class PerfCounter {

    private Logger logger;
    private long lastReport;
    // reporting interval
    private long rptInterval;
    private long baseTime;
    private long interval_start;
    private int  eventCount;
    private double rate;

    public PerfCounter(Logger logger, long rptInterval) {
        super();
        long t = System.currentTimeMillis();
        long nt = System.nanoTime();
        baseTime = t;
        interval_start = t;
        this.logger = logger;
        this.rptInterval = rptInterval;
        lastReport = baseTime;
    }

    // event time in milliseconds
    public void event(long t) {
        eventCount++;
        if ((t - interval_start) >= rptInterval) {
            interval_start = t;
            rate = 1000.0 / (rptInterval / eventCount);  // Hz
            eventCount = 0;
            logger.log(Level.INFO, String.format("msg_hil receive rate: %5.1f\n",
                    rate));
        }
    }

    private double elapsed_seconds(long curMillis) {
        return (curMillis - baseTime) / 1000.0;
    }

}
