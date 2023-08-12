/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.throttling;

import io.netty.channel.ChannelHandler;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.plugins.NetworkPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.throttling.admissioncontroller.AdmissionControllerRestHandler;
import org.opensearch.throttling.admissioncontroller.AdmissionControllerTransportInterceptor;
import org.opensearch.transport.Netty4HandlerExtension;
import org.opensearch.transport.TransportInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenSearchThrottlingModulePlugin extends Plugin implements NetworkPlugin, Netty4HandlerExtension {

    private static final Map<String, ChannelHandler> HANDLERS = new HashMap<String, ChannelHandler>();

    @Override
    public List<TransportInterceptor> getTransportInterceptors(NamedWriteableRegistry namedWriteableRegistry, ThreadContext threadContext) {
        List<TransportInterceptor> interceptors = new ArrayList<>(0);
        interceptors.add(new AdmissionControllerTransportInterceptor(null));
        return interceptors;
    }

    @Override
    public Map<String, ChannelHandler> getHandlers() {
        if (HANDLERS.isEmpty()) {
            HANDLERS.put("opensearch-throttling-plugin:AdmissionControlRestHandler", new AdmissionControllerRestHandler());
        }
        return HANDLERS;
    }
}
