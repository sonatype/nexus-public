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
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredRegion;

public class S3BlobStoreConfigurationHelperTest
    extends TestSupport
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
    withRegion(Regions.US_EAST_1, () -> assertThat(getConfiguredBucket(configuration), is("us-e-bucket")));

    // Running region doesn't match anything, use primary
    withRegion(Regions.AF_SOUTH_1, () -> assertThat(getConfiguredBucket(configuration), is("def-bucket")));
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
    withRegion(Regions.US_EAST_1, () -> assertThat(getConfiguredRegion(configuration), is("us-east-1")));

    // Running region doesn't match anything, use primary
    withRegion(Regions.AF_SOUTH_1, () -> assertThat(getConfiguredRegion(configuration), is("default")));
  }

  private static void withRegion(final Regions regions, final Runnable runnable) {
    S3BlobStoreConfigurationHelper.region = null;
    S3BlobStoreConfigurationHelper.regionLoaded = false;

    try (MockedStatic<Regions> regionsMock = mockStatic(Regions.class)) {

      Region region = null;
      if (regions != null) {
        region = mock(Region.class);
        when(region.getName()).thenReturn(regions.getName());
      }
      when(Regions.getCurrentRegion()).thenReturn(region);
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
        .region(region)
        .expiration(1);

    if (failoverBuckets != null) {
      failoverBuckets.forEach((failoverRegion, failoverBucket) -> builder.failover(failoverRegion, failoverBucket));
    }

    return builder.build();
  }
}
