/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.node.resource.tracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.monitor.fs.FsService;
import org.opensearch.node.IoUsageStats;
import org.opensearch.threadpool.ThreadPool;

/**
 * AverageIoUsageTracker tracks the IO usage by polling the FS Stats for IO metrics every (pollingInterval)
 * and keeping track of the rolling average over a defined time window (windowDuration).
 */
public class AverageIoUsageTracker extends AbstractAverageUsageTracker {

    private static final Logger LOGGER = LogManager.getLogger(AverageIoUsageTracker.class);
    private final FsService fsService;
    private long prevIoTimeMillis;
    private long prevTimeMillis;
    private final IoUsageStats ioUsageStats;

    public AverageIoUsageTracker(FsService fsService, ThreadPool threadPool, TimeValue pollingInterval, TimeValue windowDuration) {
        super(threadPool, pollingInterval, windowDuration);
        this.fsService = fsService;
        this.prevIoTimeMillis = -1;
        this.prevTimeMillis = -1;
        this.ioUsageStats = new IoUsageStats(-1);
    }

    /**
     * Get current IO usage percentage calculated using fs stats
     */
    @Override
    public long getUsage() {
        long usage = 0;
        if (this.preValidateFsStats()) {
            return usage;
        }
        long currentIoTimeMillis = fsService.stats().getIoStats().getTotalIOTimeMillis();
        long ioDevicesCount = fsService.stats().getIoStats().getDevicesStats().length;
        long currentTimeMillis = fsService.stats().getTimestamp();
        if (prevTimeMillis > 0 && (currentTimeMillis - this.prevTimeMillis > 0)) {
            long averageIoTime = (currentIoTimeMillis - this.prevIoTimeMillis) / ioDevicesCount;
            usage = averageIoTime * 100 / (currentTimeMillis - this.prevTimeMillis);
        }
        this.prevTimeMillis = currentTimeMillis;
        this.prevIoTimeMillis = currentIoTimeMillis;
        return usage;
    }

    @Override
    protected void doStart() {
        scheduledFuture = threadPool.scheduleWithFixedDelay(() -> {
            long usage = getUsage();
            recordUsage(usage);
            updateIoUsageStats();
        }, pollingInterval, ThreadPool.Names.GENERIC);
    }

    private boolean preValidateFsStats() {
        return fsService == null
            || fsService.stats() == null
            || fsService.stats().getIoStats() == null
            || fsService.stats().getIoStats().getDevicesStats() == null;
    }

    private void updateIoUsageStats() {
        this.ioUsageStats.setIoUtilisationPercent(this.isReady() ? this.getAverage() : -1);
    }

    public IoUsageStats getIoUsageStats() {
        return this.ioUsageStats;
    }
}
