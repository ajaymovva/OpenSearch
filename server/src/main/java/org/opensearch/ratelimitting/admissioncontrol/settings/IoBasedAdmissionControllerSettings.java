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
import org.opensearch.ratelimitting.admissioncontrol.RejectionSettingsListener;
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

    /**
     * Default parameters for the IoBasedAdmissionControllerSettings
     */
    public static class Defaults {
        public static final long IO_USAGE_LIMIT = 95;
        private static final double REJECTION_RATIO = 0.25;
        private static final double REJECTION_BURST = 5.0;
        public static final long MAX_IO_USAGE_LIMIT = 99;
        public static final long REJECTION_RATIO_RATE = 2;
    }

    private AdmissionControlMode transportLayerMode;
    private Long searchIOUsageLimit;
    private final List<RejectionSettingsListener> listeners = new ArrayList<>();
    private Long indexingIOUsageLimit;
    private volatile double rejectionRatio;
    private volatile double actualRejectionRatio;
    private volatile double rejectionBurst;
    private volatile long maxIoUsageLimit;
    private volatile long rejectionRatioRate;

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
     * This setting used to set the IO Limits for the search requests by default it will use default IO usage limit
     */
    public static final Setting<Long> MAX_IO_USAGE_LIMIT = Setting.longSetting(
        "admission_control.max_io_usage.limit",
        Defaults.MAX_IO_USAGE_LIMIT,
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

    public static final Setting<Long> SETTING_REJECTION_RATIO_RATE = Setting.longSetting(
        "admission_control.io_usage.rejection_ratio_rate",
        Defaults.REJECTION_RATIO_RATE,
        1,
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
        this.rejectionRatio = SETTING_REJECTION_RATIO.get(settings);
        this.maxIoUsageLimit = MAX_IO_USAGE_LIMIT.get(settings);
        this.rejectionRatioRate = SETTING_REJECTION_RATIO_RATE.get(settings);
        this.actualRejectionRatio = SETTING_REJECTION_RATIO.get(settings);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_BURST, this::setRejectionBurst);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_RATIO, this::setActualRejectionRatio);
        clusterSettings.addSettingsUpdateConsumer(MAX_IO_USAGE_LIMIT, this::setMaxIoUsageLimit);
        clusterSettings.addSettingsUpdateConsumer(SETTING_REJECTION_RATIO_RATE, this::setRejectionRatioRate);
    }

    public void addListener(RejectionSettingsListener listener) {
        listeners.add(listener);
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

    public double getMaxIoUsageLimit() {
        return maxIoUsageLimit;
    }

    public void setMaxIoUsageLimit(long maxIoUsageLimit) {
        this.maxIoUsageLimit = maxIoUsageLimit;
    }

    public double getRejectionBurst() {
        return rejectionBurst;
    }

    public double getRejectionRatio() {
        return rejectionRatio;
    }

    public long getRejectionRatioRate() {
        return rejectionRatioRate;
    }

    public void setRejectionRatioRate(long rejectionRatioRate) {
        this.rejectionRatioRate = rejectionRatioRate;
    }

    public void setRejectionBurst(double rejectionBurst) {
        this.rejectionBurst = rejectionBurst;
        this.updateListeners();
    }

    public double getActualRejectionRatio() {
        return actualRejectionRatio;
    }

    public void setActualRejectionRatio(double actualRejectionRatio) {
        this.actualRejectionRatio = actualRejectionRatio;
        this.setRejectionRatio(actualRejectionRatio);
    }

    public void setRejectionRatio(double rejectionRatio) {
        if( rejectionRatio > 0) {
            this.rejectionRatio = rejectionRatio;
            this.updateListeners();
        }
    }

    private void updateListeners() {
        this.listeners.forEach(RejectionSettingsListener::onSettingsChanged);
    }
}
