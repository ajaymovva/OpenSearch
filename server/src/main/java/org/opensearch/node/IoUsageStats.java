/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.node;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * This class is to store tne IO Usage Stats and used to return in node stats API.
 */
public class IoUsageStats implements Writeable, ToXContentFragment {

    private double ioUtilisationPercent;
    private double ewmaioUtilisationPercent;

    public IoUsageStats(double ioUtilisationPercent) {
        this.ioUtilisationPercent = ioUtilisationPercent;
        this.ewmaioUtilisationPercent = ioUtilisationPercent;
    }

    /**
     *
     * @param in the stream to read from
     * @throws IOException if an error occurs while reading from the StreamOutput
     */
    public IoUsageStats(StreamInput in) throws IOException {
        this.ioUtilisationPercent = in.readDouble();
        this.ewmaioUtilisationPercent = in.readDouble();
    }

    /**
     * Write this into the {@linkplain StreamOutput}.
     *
     * @param out the output stream to write entity content to
     */
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeDouble(this.ioUtilisationPercent);
        out.writeDouble(this.ewmaioUtilisationPercent);
    }

    public double getIoUtilisationPercent() {
        return ioUtilisationPercent;
    }

    public double getEWMAioUtilisationPercent() {
        return  ewmaioUtilisationPercent;
    }

    public void setIoUtilisationPercent(double ioUtilisationPercent) {
        this.ioUtilisationPercent = ioUtilisationPercent;
    }

    public void setEWMAioUtilisationPercent(double ewmaioUtilisationPercent) {
        this.ewmaioUtilisationPercent = ewmaioUtilisationPercent;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("io_utilization_percent", String.format(Locale.ROOT, "%.1f", this.ioUtilisationPercent));
        return builder.endObject();
    }

    @Override
    public String toString() {
        return "IO utilization percent: " + String.format(Locale.ROOT, "%.1f", this.ioUtilisationPercent);
    }
}
