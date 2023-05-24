/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.monitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.ExponentiallyWeightedMovingAverage;
import org.opensearch.common.component.AbstractLifecycleComponent;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.monitor.jvm.JvmStats;
import org.opensearch.monitor.process.ProcessProbe;
import org.opensearch.threadpool.Scheduler;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.*;

public class PerfStatsMonitorService extends AbstractLifecycleComponent {

    private static final Logger logger = LogManager.getLogger(PerfStatsMonitorService.class);
    private final ThreadPool threadPool;
    private final TimeValue refreshInterval;
    private volatile Scheduler.Cancellable scheduledFuture;
    private final AdmissionControllerService admissionControllerService;
    private final Map<String, Queue<NodePerfStats>> nodePerfStatsMap;
    double EWMA_ALPHA = 0.3;
    private final ExponentiallyWeightedMovingAverage cpuExecutionEWMA;
    private final ExponentiallyWeightedMovingAverage memoryExecutionEWMA;
    private final ExponentiallyWeightedMovingAverage ioExecutionEWMA;

    public static final Setting<TimeValue> REFRESH_INTERVAL_SETTING = Setting.timeSetting(
        "perfstatsmonitor.io.monitor.refresh_interval",
        TimeValue.timeValueSeconds(100),
        TimeValue.timeValueMillis(1),
        Setting.Property.NodeScope
    );

    public PerfStatsMonitorService(Settings settings, AdmissionControllerService admissionControllerService, ThreadPool threadPool) {
        this.admissionControllerService = admissionControllerService;
        this.nodePerfStatsMap = new HashMap<>();
        this.threadPool = threadPool;
        this.refreshInterval = REFRESH_INTERVAL_SETTING.get(settings);
        this.cpuExecutionEWMA = new ExponentiallyWeightedMovingAverage(EWMA_ALPHA, 0);
        this.memoryExecutionEWMA = new ExponentiallyWeightedMovingAverage(EWMA_ALPHA, 0);
        this.ioExecutionEWMA = new ExponentiallyWeightedMovingAverage(EWMA_ALPHA, 0);
    }

    public void setNodePerfStats(String nodeId, String perfStats) {
        Map<String, Object> statsMap = XContentHelper.convertToMap(JsonXContent.jsonXContent, perfStats, false);
        NodePerfStats nodePerfStats = new NodePerfStats((Double) statsMap.get("CPU"), (Double) statsMap.get("IO"), (Double) statsMap.get("JVM"));
        Queue <NodePerfStats> nodePerfStatsQueue = this.nodePerfStatsMap.getOrDefault(nodeId, new LinkedList<>());
        nodePerfStatsQueue.add(nodePerfStats);
        if (nodePerfStatsQueue.size() > 10) {
            nodePerfStatsQueue.remove();
        }
        this.nodePerfStatsMap.put(nodeId, nodePerfStatsQueue);
        if (nodePerfStats.cpuPercentAvg > 90 || nodePerfStats.memoryPercentAvg > 80) {
            this.admissionControllerService.updateNodeAdmissionControllerStatus(nodeId, AdmissionControllerService.AdmissionControllerState.OPEN);
        } else if (nodePerfStats.cpuPercentAvg > 70 || nodePerfStats.memoryPercentAvg > 70) {
            this.admissionControllerService.updateNodeAdmissionControllerStatus(nodeId, AdmissionControllerService.AdmissionControllerState.HALF_OPEN);
        }else {
            AdmissionControllerService.AdmissionControllerState admissionControllerState = this.admissionControllerService.getAdmissionControllerState(nodeId);
            if (admissionControllerState != AdmissionControllerService.AdmissionControllerState.CLOSED) {
                this.admissionControllerService.updateNodeAdmissionControllerStatus(nodeId, AdmissionControllerService.AdmissionControllerState.CLOSED);
            }
        }
    }

    @Override
    protected void doStart() {
        this.scheduledFuture = threadPool.scheduleWithFixedDelay(new PerfStatsMonitorService.PerfStatsMonitor(this.admissionControllerService), refreshInterval, ThreadPool.Names.GENERIC);
    }

    @Override
    protected void doStop() {
        this.scheduledFuture.cancel();
    }

    @Override
    protected void doClose() throws IOException {

    }

    public double getCPUEWMA() {
        return cpuExecutionEWMA.getAverage();
    }

    public double getMemoryEWMA() {
        return memoryExecutionEWMA.getAverage();
    }

    public double getIOEWMA() {
        return ioExecutionEWMA.getAverage();
    }

    class PerfStatsMonitor implements Runnable {

        private final AdmissionControllerService admissionControllerService;
        PerfStatsMonitor(AdmissionControllerService admissionControllerService) {
            this.admissionControllerService = admissionControllerService;
        }

        @Override
        public void run() {
            monitorIOUtilisation();
            monitorCpuUtilisation();
            monitorMemoryUtilisation();
        }

        private void monitorCpuUtilisation() {
            cpuExecutionEWMA.addValue( ProcessProbe.getInstance().getProcessCpuPercent() / 100.0);
        }
        private void monitorMemoryUtilisation() {
            memoryExecutionEWMA.addValue(JvmStats.jvmStats().getMem().getHeapUsedPercent() / 100.0);
        }
        private void monitorIOUtilisation() {
            ioExecutionEWMA.addValue(ProcessProbe.getInstance().getProcessCpuPercent() / 100.0);
        }
    }
}
