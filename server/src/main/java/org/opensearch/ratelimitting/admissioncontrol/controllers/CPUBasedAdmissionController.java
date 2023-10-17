/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.ratelimitting.admissioncontrol.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ConcurrentCollections;
import org.opensearch.core.concurrency.OpenSearchRejectedExecutionException;
import org.opensearch.node.NodeResourceUsageStats;
import org.opensearch.node.ResourceUsageCollectorService;
import org.opensearch.ratelimitting.admissioncontrol.enums.AdmissionControlActionType;
import org.opensearch.ratelimitting.admissioncontrol.enums.AdmissionControlMode;
import org.opensearch.ratelimitting.admissioncontrol.settings.CPUBasedAdmissionControllerSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Class for CPU Based Admission Controller in OpenSearch, which aims to provide CPU utilisation admission control.
 *  It provides methods to apply admission control if configured limit has been reached
 */
public class CPUBasedAdmissionController implements AdmissionController {
    private static final Logger LOGGER = LogManager.getLogger(CPUBasedAdmissionController.class);
    private final String admissionControllerName;
    public CPUBasedAdmissionControllerSettings settings;
    private final Map<String, AtomicLong> rejectionCountMap;
    private final ResourceUsageCollectorService resourceUsageCollectorService;
    private final ClusterService clusterService;

    /**
     *
     * @param admissionControllerName State of the admission controller
     */
    public CPUBasedAdmissionController(String admissionControllerName, Settings settings, ClusterService clusterService, ResourceUsageCollectorService resourceUsageCollectorService) {
        this.admissionControllerName = admissionControllerName;
        this.settings = new CPUBasedAdmissionControllerSettings(clusterService.getClusterSettings(), settings);
        this.rejectionCountMap = ConcurrentCollections.newConcurrentMap();
        this.resourceUsageCollectorService = resourceUsageCollectorService;
        this.clusterService = clusterService;
    }

    /**
     *
     * @return true if admissionController is enabled for the transport layer else false
     */
    @Override
    public boolean isEnabledForTransportLayer() {
        return this.settings.getTransportLayerAdmissionControllerMode() != AdmissionControlMode.DISABLED;
    }

    /**
     * This function will take of applying admission controller based on CPU usage
     * @param action is the transport action
     */
    @Override
    public void apply(String action, AdmissionControlActionType transportActionType) {
        // TODO Will extend this logic further currently just incrementing rejectionCount
        if (this.isEnabledForTransportLayer()) {
            this.applyForTransportLayer(action, transportActionType);
        }
    }

    private void applyForTransportLayer(String actionName, AdmissionControlActionType transportActionType) {
        if (isLimitsBreached(transportActionType)) {
            this.addRejectionCount(transportActionType.getType(), 1);
            if (this.isAdmissionControllerEnforced()) {
                throw new OpenSearchRejectedExecutionException("Action ["+ actionName +"] was rejected due to CPU usage admission controller limit breached");
            }
        }
    }

    private boolean isLimitsBreached(AdmissionControlActionType transportActionType) {
        long maxCpuLimit = this.getMaxCPULimitForAction(transportActionType);
        Optional<NodeResourceUsageStats> nodePerformanceStatistics = this.resourceUsageCollectorService.getNodeStatistics(this.clusterService.state().nodes().getLocalNodeId());
        if(nodePerformanceStatistics.isPresent()) {
            double cpuUsage = nodePerformanceStatistics.get().getCpuUtilizationPercent();
            if (cpuUsage >= maxCpuLimit){
                LOGGER.warn("CpuBasedAdmissionController rejected the request as the current CPU usage [" +
                    cpuUsage + "%] exceeds the allowed limit [" + maxCpuLimit + "%]");
                return true;
            }
        }
        return false;
    }

    private long getMaxCPULimitForAction(AdmissionControlActionType transportActionType) {
        switch (transportActionType) {
            case SEARCH:
                return this.settings.getSearchCPULimit();
            case INDEXING:
                return this.settings.getIndexingCPULimit();
            default:
                throw new IllegalArgumentException("Not Supported TransportAction Type: " + transportActionType.getType());
        }
    }

    /**
     *
     * @return true if admissionController is Enforced Mode else false
     */
    public Boolean isAdmissionControllerEnforced() {
        return this.settings.getTransportLayerAdmissionControllerMode() == AdmissionControlMode.ENFORCED;
    }

    /**
     * @return name of the admission Controller
     */
    @Override
    public String getName() {
        return this.admissionControllerName;
    }

    public void addRejectionCount(String actionType, long count) {
        AtomicLong updatedCount = new AtomicLong(0);
        if(this.rejectionCountMap.containsKey(actionType)){
            updatedCount.addAndGet(this.rejectionCountMap.get(actionType).get());
        }
        updatedCount.addAndGet(count);
        this.rejectionCountMap.put(actionType, updatedCount);
    }

    /**
     * @return current value of the rejection count metric tracked by the admission-controller.
     */
    @Override
    public long getRejectionCount(String actionType) {
        AtomicLong rejectionCount = this.rejectionCountMap.getOrDefault(actionType, new AtomicLong());
        return rejectionCount.get();
    }

    public Map<String, Long> getRejectionStats() {
        Map<String, Long> rejectionStats = new HashMap<>();
        rejectionCountMap.forEach((actionType, count) -> rejectionStats.put(actionType, count.get()));
        return rejectionStats;
    }
}
