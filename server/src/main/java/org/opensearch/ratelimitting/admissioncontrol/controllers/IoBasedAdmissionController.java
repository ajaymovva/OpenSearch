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
import org.opensearch.common.util.TokenBucket;
import org.opensearch.core.concurrency.OpenSearchRejectedExecutionException;
import org.opensearch.node.NodeResourceUsageStats;
import org.opensearch.node.ResourceUsageCollectorService;
import org.opensearch.ratelimitting.admissioncontrol.RejectionSettingsListener;
import org.opensearch.ratelimitting.admissioncontrol.enums.AdmissionControlActionType;
import org.opensearch.ratelimitting.admissioncontrol.settings.IoBasedAdmissionControllerSettings;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class IoBasedAdmissionController extends AdmissionController implements RejectionSettingsListener {
    public static final String IO_BASED_ADMISSION_CONTROLLER = "global_io_usage";
    private static final Logger LOGGER = LogManager.getLogger(IoBasedAdmissionController.class);
    public IoBasedAdmissionControllerSettings settings;
    private final AtomicReference<TokenBucket> ratioLimiter;
    private final AtomicLong completionCount;
    private long lastUpdatedTimeStampOnRejection;
    private double lastUpdateIOUsageOnRejection;

    /**
     * @param admissionControllerName       name of the admissionController
     * @param resourceUsageCollectorService instance used to get resource usage stats of the node
     * @param clusterService                instance of the clusterService
     */
    public IoBasedAdmissionController(
        String admissionControllerName,
        ResourceUsageCollectorService resourceUsageCollectorService,
        ClusterService clusterService,
        Settings settings
    ) {
        super(admissionControllerName, resourceUsageCollectorService, clusterService);
        this.settings = new IoBasedAdmissionControllerSettings(clusterService.getClusterSettings(), settings);
        this.completionCount = new AtomicLong();
        this.ratioLimiter = new AtomicReference<>(new TokenBucket(this::getCompletionCount, this.settings.getRejectionRatio(), this.settings.getRejectionBurst()));
        this.lastUpdatedTimeStampOnRejection = -1;
        this.lastUpdateIOUsageOnRejection = -1;
        this.settings.addListener(this);
    }

    /**
     * Apply admission control based on the resource usage for an action
     *
     * @param action is the transport action
     * @param admissionControlActionType type of admissionControlActionType
     */
    @Override
    public void apply(String action, AdmissionControlActionType admissionControlActionType) {
        if (this.isEnabledForTransportLayer(this.settings.getTransportLayerAdmissionControllerMode())) {
            this.applyForTransportLayer(action, admissionControlActionType);
        }
    }

    /**
     * Apply transport layer admission control if configured limit has been reached
     */
    private void applyForTransportLayer(String actionName, AdmissionControlActionType admissionControlActionType) {
        if (isEligibleForRejection(actionName, admissionControlActionType)) {
            this.addRejectionCount(admissionControlActionType.getType(), 1);
            if (this.isAdmissionControllerEnforced(this.settings.getTransportLayerAdmissionControllerMode())) {
                throw new OpenSearchRejectedExecutionException(
                    String.format(
                        Locale.ROOT,
                        "IO usage admission controller rejected the request for action [%s] as IO Usage limit reached",
                        admissionControlActionType.name()
                    )
                );
            }
        }
        this.completionCount.incrementAndGet();
    }

    /**
     * Check if the configured resource usage limits are breached for the action
     */
    private boolean isEligibleForRejection(String actionName, AdmissionControlActionType admissionControlActionType) {
        // check if cluster state is ready
        if (clusterService.state() != null && clusterService.state().nodes() != null) {
            Optional<NodeResourceUsageStats> nodePerformanceStatistics = this.resourceUsageCollectorService.getNodeStatistics(
                this.clusterService.state().nodes().getLocalNodeId()
            );
            if (nodePerformanceStatistics.isPresent()) {
                double ioUsage = nodePerformanceStatistics.get().getIoUsageStats().getIoUtilisationPercent();
                if(ioUsage > this.settings.getMaxIoUsageLimit()) {
                    LOGGER.warn(
                        "IoBasedAdmissionController limit reached as the current IO "
                            + "usage [{}] exceeds the max allowed limit [{}] in admissionControlMode [{}]",
                        ioUsage,
                        this.settings.getMaxIoUsageLimit(),
                        this.settings.getTransportLayerAdmissionControllerMode()
                    );
                    return true;
                }
                long maxIoLimitForAction = this.getIoRejectionThreshold(admissionControlActionType);
                if (ioUsage >= maxIoLimitForAction) {
                    boolean ratioLimitNotReached = this.ratioLimiter.get().request();
                    this.tuneRejectionMetrics(ioUsage);
                    if (ratioLimitNotReached) {
                        LOGGER.warn(
                            "IoBasedAdmissionController limit reached as the current IO "
                                + "usage [{}] exceeds the allowed limit [{}] for transport action [{}] in admissionControlMode [{}]",
                            ioUsage,
                            maxIoLimitForAction,
                            actionName,
                            this.settings.getTransportLayerAdmissionControllerMode()
                        );
                        return true;
                    }
                } else {
                    if (this.settings.getActualRejectionRatio() != this.settings.getRejectionRatio()){
                        this.settings.setRejectionRatio(this.settings.getActualRejectionRatio());
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get IO rejection threshold based on action type
     */
    private long getIoRejectionThreshold(AdmissionControlActionType admissionControlActionType) {
        switch (admissionControlActionType) {
            case SEARCH:
                return this.settings.getSearchIOUsageLimit();
            case INDEXING:
                return this.settings.getIndexingIOUsageLimit();
            default:
                throw new IllegalArgumentException(
                    String.format(
                        Locale.ROOT,
                        "Admission control not Supported for AdmissionControlActionType: %s",
                        admissionControlActionType.getType()
                    )
                );
        }
    }

    public long getCompletionCount() {
        return completionCount.get();
    }

    @Override
    public void onSettingsChanged() {
        this.ratioLimiter.set(new TokenBucket(this::getCompletionCount, this.settings.getRejectionRatio(), this.settings.getRejectionBurst()));
    }

    private void tuneRejectionMetrics(double ioUsage) {
        if(this.lastUpdatedTimeStampOnRejection > 0) {
            long diffTime = (System.currentTimeMillis() - this.lastUpdatedTimeStampOnRejection) / 1000;
            if (diffTime >= 15) {
                if(ioUsage >= this.lastUpdateIOUsageOnRejection) {
                    this.settings.setRejectionRatio(this.settings.getRejectionRatio() * this.settings.getRejectionRatioRate());
                } else {
                    this.settings.setRejectionRatio(this.settings.getRejectionRatio() / this.settings.getRejectionRatioRate());
                }
                this.lastUpdatedTimeStampOnRejection = System.currentTimeMillis();
                this.lastUpdateIOUsageOnRejection = ioUsage;
            }
        } else {
            this.lastUpdatedTimeStampOnRejection = System.currentTimeMillis();
            this.lastUpdateIOUsageOnRejection = ioUsage;
        }
    }
}
