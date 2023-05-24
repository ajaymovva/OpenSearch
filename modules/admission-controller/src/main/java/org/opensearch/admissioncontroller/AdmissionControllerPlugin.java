/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.admissioncontroller;

import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.plugins.NetworkPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.transport.TransportInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up classes for node/shard level admission controller.
 * Provides abstraction and orchestration for admission controller interfaces when called from Transport Actions or for Stats.
 *
 * @opensearch.internal
 */
public class AdmissionControllerPlugin extends Plugin implements NetworkPlugin {

    /**
     * Sets up classes for node/shard level admission controller.
     * Provides abstraction and orchestration for admission controller interfaces when called from Transport Actions or for Stats.
     *
     * @opensearch.internal
     */
    public AdmissionControllerPlugin() {
    }

    /**
     * Sets up classes for node/shard level admission controller.
     * Provides abstraction and orchestration for admission controller interfaces when called from Transport Actions or for Stats.
     *
     * @opensearch.internal
     * @param namedWriteableRegistry used for later
     * @param threadContext used for later
     */

    @Override
    public List<TransportInterceptor> getTransportInterceptors(NamedWriteableRegistry namedWriteableRegistry, ThreadContext threadContext) {
        List<TransportInterceptor> interceptors = new ArrayList<TransportInterceptor>(0);
//        interceptors.add(new AdmissionControllerTransportInterceptor(null));
        return interceptors;
    }
}
