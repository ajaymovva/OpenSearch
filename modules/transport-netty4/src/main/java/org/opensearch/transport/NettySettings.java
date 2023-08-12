/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport;

import org.opensearch.common.settings.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class NettySettings {
    private NettySettings() {}

    // TODO: Evaluate which one is better, Moving this to yml(AMI Config) or keeping it here.
    public static final Setting<List<String>> HANDLER_ORDERING = Setting.listSetting(
        "opensearch.netty.plugin.handler.ordering",
        Arrays.asList("opensearch-throttling-plugin:AdmissionControlRestHandler"),
        Function.identity(),
        Setting.Property.NodeScope
    );
}
