/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport;

import io.netty.channel.ChannelHandler;

import java.util.Collections;
import java.util.Map;

public interface Netty4HandlerExtension {
    default Map<String, ChannelHandler> getHandlers() {
        return Collections.emptyMap();
    }
}
