/**
 * Copyright (c) 2012-2015, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.dynamo;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;

/**
 * Amazon DynamoDB credentials.
 *
 * <p>It is recommended to use {@link Credentials.Simple} in most cases.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@Immutable
public interface Credentials {

    /**
     * Test credentials, for unit testing mostly.
     */
    Credentials TEST = new Credentials.Simple(
        "AAAAAAAAAAAAAAAAAAAA",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    );

    /**
     * Build AWS client.
     *
     * <p>Don't forget to shut it down after use,
     * using {@link AmazonDynamoDB#shutdown()}.
     *
     * @return Amazon Dynamo DB client
     */
    @NotNull(message = "AWS DynamoDB client is never NULL")
    AmazonDynamoDB aws();

    /**
     * Simple implementation.
     */
    @Immutable
    @Loggable(Loggable.DEBUG)
    @EqualsAndHashCode(of = { "key", "secret", "region" })
    final class Simple implements Credentials {
        /**
         * AWS key.
         */
        private final transient String key;
        /**
         * AWS secret.
         */
        private final transient String secret;
        /**
         * Region name.
         */
        private final transient String region;
        /**
         * Public ctor, with "us-east-1" region.
         * @param akey AWS key
         * @param scrt Secret
         */
        public Simple(
            @NotNull(message = "key can't be NULL") final String akey,
            @NotNull(message = "secret can't be NULL") final String scrt) {
            this(akey, scrt, Regions.US_EAST_1.getName());
        }
        /**
         * Public ctor.
         * @param akey AWS key
         * @param scrt Secret
         * @param reg Region
         */
        public Simple(
            @NotNull(message = "key can't be NULL")
            @Pattern(regexp = "[A-Z0-9]{20}")
            final String akey,
            @NotNull(message = "secret can't be NULL")
            @Pattern(regexp = "[a-zA-Z0-9+/=]{40}")
            final String scrt,
            @NotNull(message = "region can't be NULL")
            @Pattern(regexp = "[-a-z0-9]+")
            final String reg) {
            this.key = akey;
            this.secret = scrt;
            this.region = reg;
        }
        @Override
        @NotNull(message = "String cannot be null")
        public String toString() {
            return String.format("%s/%s", this.region, this.key);
        }
        @Override
        @NotNull(message = "AWS client is never NULL")
        public AmazonDynamoDB aws() {
            final com.amazonaws.regions.Region reg =
                RegionUtils.getRegion(this.region);
            if (reg == null) {
                throw new IllegalStateException(
                    String.format("Failed to find region '%s'", this.region)
                );
            }
            final AmazonDynamoDB aws = new AmazonDynamoDBClient(
                new BasicAWSCredentials(this.key, this.secret)
            );
            aws.setRegion(reg);
            return aws;
        }
    }

    /**
     * Assumed AWS IAM role.
     *
     * @see <a href="http://docs.aws.amazon.com/IAM/latest/UserGuide/role-usecase-ec2app.html">Granting Applications that Run on Amazon EC2 Instances Access to AWS Resources</a>
     */
    @Immutable
    @Loggable(Loggable.DEBUG)
    @EqualsAndHashCode(of = "region")
    final class Assumed implements Credentials {
        /**
         * Region name.
         */
        private final transient String region;
        /**
         * Public ctor.
         */
        public Assumed() {
            this(Regions.US_EAST_1.getName());
        }
        /**
         * Public ctor.
         * @param reg Region
         */
        public Assumed(
            @NotNull(message = "DynamoDB region can't be NULL")
            @Pattern(regexp = "[-0-9a-z]+")
            final String reg) {
            this.region = reg;
        }
        @Override
        @NotNull(message = "String cannot be null")
        public String toString() {
            return this.region;
        }
        @Override
        @NotNull(message = "AWS client is never NULL")
        public AmazonDynamoDB aws() {
            final com.amazonaws.regions.Region reg =
                RegionUtils.getRegion(this.region);
            if (reg == null) {
                throw new IllegalStateException(
                    String.format("Failed to detect region '%s'", this.region)
                );
            }
            final AmazonDynamoDB aws = new AmazonDynamoDBClient();
            aws.setRegion(reg);
            return aws;
        }
    }

    /**
     * With explicitly specified endpoint.
     */
    @Immutable
    @Loggable(Loggable.DEBUG)
    @EqualsAndHashCode(of = { "origin", "endpoint" })
    final class Direct implements Credentials {
        /**
         * Original credentials.
         */
        private final transient Credentials origin;
        /**
         * Endpoint.
         */
        private final transient String endpoint;
        /**
         * Public ctor.
         * @param creds Original credentials
         * @param pnt Endpoint
         */
        public Direct(
            @NotNull(message = "creds can't be NULL") final Credentials creds,
            @NotNull(message = "endpoint can't be NULL") final String pnt) {
            this.origin = creds;
            this.endpoint = pnt;
        }
        /**
         * Public ctor.
         * @param creds Original credentials
         * @param port Port number for localhost
         */
        public Direct(@NotNull(message = "creds can't be NULL")
            final Credentials creds, final int port) {
            this(creds, String.format("http://localhost:%d", port));
        }
        @Override
        @NotNull(message = "String cannot be null")
        public String toString() {
            return String.format("%s at %s", this.origin, this.endpoint);
        }
        @Override
        @NotNull(message = "AWS client is never NULL")
        public AmazonDynamoDB aws() {
            final AmazonDynamoDB aws = this.origin.aws();
            aws.setEndpoint(this.endpoint);
            return aws;
        }
    }
}
