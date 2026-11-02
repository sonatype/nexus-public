/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.blobstore.s3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.regions.Region;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CLOUDFRONT_DISTRIBUTION_URL_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CLOUDFRONT_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredRegion;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.TRANSFER_ACCELERATION_ENABLED_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.isGlobalDistributionEnabled;

public class S3BlobStoreConfigurationHelperTest
{
  @Test
  public void testGetConfiguredBucket() {
    // null doesn't cause failure
    withRegion(null,
        () -> assertThat(getConfiguredBucket(configuration("default", "def-bucket", null)), is("def-bucket")));

    // empty doesn't cause failure
    withRegion(null,
        () -> assertThat(getConfiguredBucket(configuration("default", "def-bucket", Collections.emptyMap())),
            is("def-bucket")));

    BlobStoreConfiguration configuration =
        configuration("default", "def-bucket", ImmutableMap.of("us-east-1", "us-e-bucket"));

    // no region should use primary
    withRegion(null, () -> assertThat(getConfiguredBucket(configuration), is("def-bucket")));

    // Match fail over region
    withRegion(Region.US_EAST_1, () -> assertThat(getConfiguredBucket(configuration), is("us-e-bucket")));

    // Running region doesn't match anything, use primary
    withRegion(Region.AF_SOUTH_1, () -> assertThat(getConfiguredBucket(configuration), is("def-bucket")));
  }

  @Test
  public void testGetConfiguredRegion() {
    // null doesn't cause failure
    withRegion(null,
        () -> assertThat(getConfiguredRegion(configuration("default", "def-bucket", null)), is("default")));

    // empty doesn't cause failure
    withRegion(null,
        () -> assertThat(getConfiguredRegion(configuration("default", "def-bucket", Collections.emptyMap())),
            is("default")));

    BlobStoreConfiguration configuration =
        configuration("default", "def-bucket", ImmutableMap.of("us-east-1", "us-e-bucket"));

    // no region should use primary
    withRegion(null, () -> assertThat(getConfiguredRegion(configuration), is("default")));

    // Match fail over region
    withRegion(Region.US_EAST_1, () -> assertThat(getConfiguredRegion(configuration), is("us-east-1")));

    // Running region doesn't match anything, use primary
    withRegion(Region.AF_SOUTH_1, () -> assertThat(getConfiguredRegion(configuration), is("default")));
  }

  @Test
  public void testIsGlobalDistributionEnabled() {
    // not an S3 blob store
    assertThat(isGlobalDistributionEnabled(configuration("File", ImmutableMap.of())), is(false));

    // S3 without CloudFront or transfer acceleration
    assertThat(isGlobalDistributionEnabled(configuration(S3BlobStore.TYPE, ImmutableMap.of())), is(false));

    assertThat(isGlobalDistributionEnabled(
        configuration(S3BlobStore.TYPE, ImmutableMap.of(TRANSFER_ACCELERATION_ENABLED_KEY, true))), is(true));

    assertThat(isGlobalDistributionEnabled(
        configuration(S3BlobStore.TYPE, ImmutableMap.of(TRANSFER_ACCELERATION_ENABLED_KEY, false))), is(false));

    // persisted as a String: AttributesMap coerces to Boolean via Boolean.valueOf
    assertThat(isGlobalDistributionEnabled(
        configuration(S3BlobStore.TYPE, ImmutableMap.of(TRANSFER_ACCELERATION_ENABLED_KEY, "true"))), is(true));

    assertThat(isGlobalDistributionEnabled(
        configuration(S3BlobStore.TYPE, ImmutableMap.of(TRANSFER_ACCELERATION_ENABLED_KEY, "false"))), is(false));

    assertThat(isGlobalDistributionEnabled(configuration(S3BlobStore.TYPE, ImmutableMap.of(CLOUDFRONT_KEY,
        ImmutableMap.of(CLOUDFRONT_DISTRIBUTION_URL_KEY, "https://d111111abcdef8.cloudfront.net")))), is(true));

    // CloudFront block present but not yet distributed
    assertThat(isGlobalDistributionEnabled(
        configuration(S3BlobStore.TYPE, ImmutableMap.of(CLOUDFRONT_KEY, ImmutableMap.of()))), is(false));
  }

  private static BlobStoreConfiguration configuration(final String type, final Map<String, Object> s3Attributes) {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    when(configuration.getType()).thenReturn(type);
    when(configuration.attributes(CONFIG_KEY))
        .thenReturn(new NestedAttributesMap(CONFIG_KEY, new HashMap<>(s3Attributes)));
    return configuration;
  }

  private static void withRegion(final Region region, final Runnable runnable) {
    S3BlobStoreConfigurationHelper.region = null;
    S3BlobStoreConfigurationHelper.regionLoaded = false;

    try (MockedStatic<DefaultAwsRegionProviderChain> regionProviderMock =
        mockStatic(DefaultAwsRegionProviderChain.class)) {
      DefaultAwsRegionProviderChain mockChain = Mockito.mock(DefaultAwsRegionProviderChain.class);

      DefaultAwsRegionProviderChain.Builder mockBuilder = Mockito.mock(DefaultAwsRegionProviderChain.Builder.class);
      when(mockBuilder.build()).thenReturn(mockChain);
      when(mockChain.getRegion()).thenReturn(region);

      regionProviderMock.when(DefaultAwsRegionProviderChain::builder).thenReturn(mockBuilder);

      runnable.run();
    }
  }

  private static BlobStoreConfiguration configuration(
      final String region,
      final String bucketName,
      final Map<String, String> failoverBuckets)
  {
    BlobStoreConfiguration configuration = mock(BlobStoreConfiguration.class);
    NestedAttributesMap attributes = new NestedAttributesMap();
    when(configuration.attributes(CONFIG_KEY)).thenReturn(attributes);

    // use the builder so we get the real code path for creating a config
    S3BlobStoreConfigurationBuilder builder = S3BlobStoreConfigurationBuilder.builder(configuration, "my-blobstore")
        .bucket(bucketName)
        .region(region);

    if (failoverBuckets != null) {
      failoverBuckets.forEach((failoverRegion, failoverBucket) -> builder.failover(failoverRegion, failoverBucket));
    }

    return builder.build();
  }
}
