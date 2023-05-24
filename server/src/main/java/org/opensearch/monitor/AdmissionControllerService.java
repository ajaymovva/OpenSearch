/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.monitor;

import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.common.settings.Settings;

import java.util.HashMap;
import java.util.Map;

public class AdmissionControllerService {

    public enum AdmissionControllerState {
        OPEN,
        CLOSED,
        HALF_OPEN,
        MONITOR,
        DISABLED
    }

    public static final Setting<TimeValue> OPEN_STATUS_INTERVAL_SETTING = Setting.timeSetting(
        "admissioncontroller.openstate.refresh_interval",
        TimeValue.timeValueSeconds(100),
        TimeValue.timeValueMillis(1),
        Setting.Property.NodeScope
    );

    private final TimeValue openStatusRefreshInterval;

    public  Map<String, AdmissionControllerState> nodeAdmissionControllerState;

    private final ThreadPool threadPool;

    public AdmissionControllerService(Settings settings, ClusterSettings clusterSettings, ThreadPool threadPool) {
        this.nodeAdmissionControllerState = new HashMap<>();
        this.threadPool = threadPool;
        this.openStatusRefreshInterval = OPEN_STATUS_INTERVAL_SETTING.get(settings);
    }
    public void updateNodeAdmissionControllerStatus(String nodeId, AdmissionControllerState admissionControllerState){
        this.nodeAdmissionControllerState.put(nodeId, admissionControllerState);
        if(admissionControllerState.equals(AdmissionControllerState.OPEN)){
            this.updateOpenStateToHalfOpen(nodeId);
            threadPool.schedule(() -> this.updateOpenStateToHalfOpen(nodeId), this.openStatusRefreshInterval, ThreadPool.Names.GENERIC);
        }
    }

    public void updateOpenStateToHalfOpen(String nodeId){
        this.nodeAdmissionControllerState.put(nodeId, AdmissionControllerState.HALF_OPEN);
    }

    public AdmissionControllerState getAdmissionControllerState(String nodeId){
        return this.nodeAdmissionControllerState.get(nodeId);
    }

    public boolean evaluateToEnforceAdmissionController(String nodeId){
        return this.nodeAdmissionControllerState.getOrDefault(nodeId, AdmissionControllerState.CLOSED) == AdmissionControllerState.OPEN;
    }
}
