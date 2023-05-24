/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.monitor;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContent;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * Node perf stats
 */
public class NodePerfStats implements Writeable {
    public double cpuPercentAvg;
    public double memoryPercentAvg;
    public double ioPercentAvg;

    public NodePerfStats(StreamInput in) throws IOException {
        this.cpuPercentAvg = in.readDouble();
        this.memoryPercentAvg = in.readDouble();
        this.ioPercentAvg = in.readDouble();
    }

    public NodePerfStats(double cpuPercentAvg, double memoryPercentAvg, double ioPercentAvg) {
        this.cpuPercentAvg = cpuPercentAvg;
        this.memoryPercentAvg = memoryPercentAvg;
        this.ioPercentAvg = ioPercentAvg;
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeDouble(cpuPercentAvg);
        out.writeDouble(memoryPercentAvg);
        out.writeDouble(ioPercentAvg);
    }

    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
            builder.startObject();
            builder.field("CPU", this.cpuPercentAvg);
            builder.field("JVM", this.memoryPercentAvg);
            builder.field("IO", this.ioPercentAvg);
            builder.endObject();
            return XContentHelper.convertToJson(BytesReference.bytes(builder), false, false, builder.contentType());
        } catch (IOException e) {
//            throw new RuntimeException(e);
            return "";
        }
    }
}
