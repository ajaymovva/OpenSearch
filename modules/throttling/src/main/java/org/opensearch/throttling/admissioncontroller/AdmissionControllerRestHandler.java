/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.throttling.admissioncontroller;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ChannelHandler.Sharable
public class AdmissionControllerRestHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LogManager.getLogger(AdmissionControllerRestHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert msg instanceof FullHttpRequest : "Invalid message type: " + msg.getClass();
        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
        String uri = getUri(fullHttpRequest);
        applyAdmissionControl(uri);
        ctx.fireChannelRead(msg);
    }

    private String getUri(FullHttpRequest fullHttpRequest) {
        return fullHttpRequest.uri();
    }

    private void applyAdmissionControl(String requestURI) {
        // apply admission controller
        // LOGGER.info("Apply Admission Controller Triggered URI: " + requestURI);
    }

    private void releaseAdmissionControl(ChannelHandlerContext ctx) {
        // release the acquired objects
        // LOGGER.info("Released Admission Controller Handler");
    }

    private long getContentLength(FullHttpRequest fullHttpRequest) {
        String contentLengthHeader = fullHttpRequest.headers().get(HttpHeaderNames.CONTENT_LENGTH);
        return contentLengthHeader == null ? 0 : Long.parseLong(contentLengthHeader);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        releaseAdmissionControl(ctx);
        super.close(ctx, promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        releaseAdmissionControl(ctx);
        super.write(ctx, msg, promise);
    }
}
