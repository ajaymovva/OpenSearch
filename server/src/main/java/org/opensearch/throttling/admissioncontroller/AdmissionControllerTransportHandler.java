/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.throttling.admissioncontroller;

import org.apache.logging.log4j.LogManager;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportChannel;
import org.opensearch.transport.TransportRequest;
import org.opensearch.transport.TransportRequestHandler;
import org.apache.logging.log4j.Logger;

public class AdmissionControllerTransportHandler<T extends TransportRequest> implements TransportRequestHandler<T> {

    private final String action;
    private final TransportRequestHandler<T> actualHandler;
    private final ThreadPool threadPool;
    protected final Logger log = LogManager.getLogger(this.getClass());

    public AdmissionControllerTransportHandler(String action, TransportRequestHandler<T> actualHandler, ThreadPool threadPool) {
        super();
        this.action = action;
        this.actualHandler = actualHandler;
        this.threadPool = threadPool;
    }

    protected ThreadContext getThreadContext() {
        if (threadPool == null) {
            return null;
        }
        return threadPool.getThreadContext();
    }

    /**
     * @param request
     * @param channel
     * @param task
     * @throws Exception
     */
    @Override
    public void messageReceived(T request, TransportChannel channel, Task task) throws Exception {
        // intercept all the transport requests here and apply admission control
        // log.info("Action:" + this.action);
        this.messageReceivedDecorate(request, actualHandler, channel, task);
    }

    protected void messageReceivedDecorate(
        final T request,
        final TransportRequestHandler<T> actualHandler,
        final TransportChannel transportChannel,
        Task task
    ) throws Exception {
        actualHandler.messageReceived(request, transportChannel, task);
    }
}
