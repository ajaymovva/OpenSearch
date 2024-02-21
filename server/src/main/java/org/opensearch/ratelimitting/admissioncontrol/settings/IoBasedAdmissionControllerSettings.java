/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.ratelimitting.admissioncontrol.settings;

import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.ratelimitting.admissioncontrol.AdmissionControlSettings;
import org.opensearch.ratelimitting.admissioncontrol.enums.AdmissionControlMode;
import org.opensearch.search.backpressure.CancellationSettingsListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Settings related to IO based admission controller.
 * @opensearch.internal
 */
public class IoBasedAdmissionControllerSettings {

    private final List<CancellationSettingsListener> listeners = new ArrayList<>();

    /**
     * Default parameters for the IoBasedAdmissionControllerSettings
     */
    public static class Defaults {
        public static final long IO_USAGE_LIMIT = 98;
        private static final double REJECTION_RATIO = 0.1;
        private static final double REJECTION_RATE = 0.003;
        private static final double REJECTION_BURST = 5.0;
    }

    private AdmissionControlMode transportLayerMode;
    private Long searchIOUsageLimit;
    private Long indexingIOUsageLimit;
    private volatile double rejectionRatio;
    private volatile double rejectionRate;
    private volatile double rejectionBurst;
    /**
     * Feature level setting to operate in shadow-mode or in enforced-mode. If enforced field is set
     * rejection will be performed, otherwise only rejection metrics will be populated.
     */
    public static final Setting<AdmissionControlMode> IO_BASED_ADMISSION_CONTROLLER_TRANSPORT_LAYER_MODE = new Setting<>(
        "admission_control.transport.io_usage.mode_override",
        AdmissionControlSettings.ADMISSION_CONTROL_TRANSPORT_LAYER_MODE,
        AdmissionControlMode::fromName,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    /**
     * This setting used to set the IO Limits for the search requests by default it will use default IO usage limit
     */
    public static final Setting<Long> SEARCH_IO_USAGE_LIMIT = Setting.longSetting(
        "admission_control.search.io_usage.limit",
        Defaults.IO_USAGE_LIMIT,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    /**
     * This setting used to set the IO limits for the indexing requests by default it will use default IO usage limit
     */
    public static final Setting<Long> INDEXING_IO_USAGE_LIMIT = Setting.longSetting(
        "admission_control.indexing.io_usage.limit",
        Defaults.IO_USAGE_LIMIT,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public static final Setting<Double> SETTING_REJECTION_RATIO = Setting.doubleSetting(
        "admission_control.io_usage.rejection_ratio",
        Defaults.REJECTION_RATIO,
        0.0,
        1.0,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public static final Setting<Double> SETTING_REJECTION_RATE = Setting.doubleSetting(
        "admission_control.io_usage.rejection_rate",
        Defaults.REJECTION_RATE,
        0.0,
        1.0,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public static final Setting<Double> SETTING_REJECTION_BURST = Setting.doubleSetting(
        "admission_control.io_usage.rejection_burst",
        Defaults.REJECTION_BURST,
        1.0,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public IoBasedAdmissionControllerSettings(ClusterSettings clusterSettings, Settings settings) {
        this.transportLayerMode = IO_BASED_ADMISSION_CONTROLLER_TRANSPORT_LAYER_MODE.get(settings);
        clusterSettings.addSettingsUpdateConsumer(IO_BASED_ADMISSION_CONTROLLER_TRANSPORT_LAYER_MODE, this::setTransportLayerMode);
        this.searchIOUsageLimit = SEARCH_IO_USAGE_LIMIT.get(settings);
        this.indexingIOUsageLimit = INDEXING_IO_USAGE_LIMIT.get(settings);
        clusterSettings.addSettingsUpdateConsumer(INDEXING_IO_USAGE_LIMIT, this::setIndexingIOUsageLimit);
        clusterSettings.addSettingsUpdateConsumer(SEARCH_IO_USAGE_LIMIT, this::setSearchIOUsageLimit);
        this.rejectionBurst = SETTING_REJECTION_BURST.get(settings);
        this.rejectionRate = SETTING_REJECTION_RATE.get(settings);
        this.rejectionRatio = SETTING_REJECTION_RATIO.get(settings);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_BURST, this::setRejectionBurst);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_RATE, this::setRejectionRate);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_RATIO, this::setRejectionRatio);
    }

    public void setIndexingIOUsageLimit(Long indexingIOUsageLimit) {
        this.indexingIOUsageLimit = indexingIOUsageLimit;
    }

    public void setSearchIOUsageLimit(Long searchIOUsageLimit) {
        this.searchIOUsageLimit = searchIOUsageLimit;
    }

    public AdmissionControlMode getTransportLayerAdmissionControllerMode() {
        return transportLayerMode;
    }

    public void setTransportLayerMode(AdmissionControlMode transportLayerMode) {
        this.transportLayerMode = transportLayerMode;
    }

    public Long getIndexingIOUsageLimit() {
        return indexingIOUsageLimit;
    }

    public Long getSearchIOUsageLimit() {
        return searchIOUsageLimit;
    }

    public double getRejectionBurst() {
        return rejectionBurst;
    }

    public double getRejectionRate() {
        return rejectionRate;
    }

    public double getRejectionRatio() {
        return rejectionRatio;
    }

    public void setRejectionBurst(double rejectionBurst) {
        this.rejectionBurst = rejectionBurst;
        this.listeners.forEach(listener -> {
            listener.onBurstChanged(rejectionBurst);
        });
    }

    public void setRejectionRate(double rejectionRate) {
        this.rejectionRate = rejectionRate;
        this.listeners.forEach(listener -> {
            listener.onRateChanged(rejectionBurst);
        });
    }

    public void setRejectionRatio(double rejectionRatio) {
        this.rejectionRatio = rejectionRatio;
        this.listeners.forEach(listener -> {
            listener.onRatioChanged(rejectionBurst);
        });
    }

    public double getRejectionRateNanos() {
        return getRejectionRate() / TimeUnit.MILLISECONDS.toNanos(1);
    }

    public void addListeners(CancellationSettingsListener listener) {
        listeners.add(listener);
    }
}
