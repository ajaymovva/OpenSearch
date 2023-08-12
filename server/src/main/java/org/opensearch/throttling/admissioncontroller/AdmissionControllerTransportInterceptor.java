/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.throttling.admissioncontroller;

import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportRequest;
import org.opensearch.transport.TransportRequestHandler;

public class AdmissionControllerTransportInterceptor implements TransportInterceptor {

    protected final ThreadPool threadPool;

    public AdmissionControllerTransportInterceptor(final ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public <T extends TransportRequest> TransportRequestHandler<T> interceptHandler(
        String action,
        String executor,
        boolean forceExecution,
        TransportRequestHandler<T> actualHandler
    ) {
        return new AdmissionControllerTransportHandler<>(action, actualHandler, threadPool);
    }
}
