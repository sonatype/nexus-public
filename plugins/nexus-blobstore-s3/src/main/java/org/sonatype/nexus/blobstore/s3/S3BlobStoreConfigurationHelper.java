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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.DEFAULT_EXPIRATION_IN_DAYS;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.EXPIRATION_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.REGION_KEY;

/**
 * Helper for retrieving S3 specific settings from a {@link BlobStoreConfiguration}.
 *
 * @since 3.16
 */
public class S3BlobStoreConfigurationHelper
{
  private static final Logger log = LoggerFactory.getLogger(S3BlobStoreConfigurationHelper.class);

  public static final String BUCKET_PREFIX_KEY = "prefix";

  public static final String BUCKET_KEY = "bucket";

  /**
   * A map of buckets to use in other regions
   */
  public static final String FAILOVER_BUCKETS_KEY = "failover-buckets";

  public static final String CONFIG_KEY = "s3";

  private S3BlobStoreConfigurationHelper() {
    // empty
  }

  public static void setConfiguredBucket(final BlobStoreConfiguration blobStoreConfiguration, final String bucket) {
    blobStoreConfiguration.attributes(CONFIG_KEY).set(BUCKET_KEY, bucket);
  }

  /**
   * Returns the configured bucket, if failover buckets are configured then the choice will depend on the EC2 region.
   */
  public static String getConfiguredBucket(final BlobStoreConfiguration blobStoreConfiguration) {
    return Iterables.getOnlyElement(getBucketConfiguration(blobStoreConfiguration).values());
  }

  /**
   * Returns the configured region for the bucket, if failover buckets are configured then the choice will depend on the EC2 region.
   */
  public static String getConfiguredRegion(final BlobStoreConfiguration blobStoreConfiguration) {
    return Iterables.getOnlyElement(getBucketConfiguration(blobStoreConfiguration).keySet());
  }

  private static Map<String, String> getBucketConfiguration(final BlobStoreConfiguration blobStoreConfiguration) {
    NestedAttributesMap config = blobStoreConfiguration.attributes(CONFIG_KEY);
    String primaryBucket = config.get(BUCKET_KEY, String.class);
    String primaryRegion = config.get(REGION_KEY, String.class);
    String currentRegion = getCurrentRegion();

    if (!config.contains(FAILOVER_BUCKETS_KEY) || currentRegion == null) {
      log.trace("No failover configuration possible");
      return Collections.singletonMap(primaryRegion, primaryBucket);
    }

    Map<String, Object> regionMapping = new LinkedHashMap<>(config.get(FAILOVER_BUCKETS_KEY, Map.class));

    // Add the primary last so we always prefer it
    regionMapping.put(primaryRegion, primaryBucket);

    log.debug("Detected region {} choosing from {}", currentRegion, regionMapping);

    return Optional.ofNullable(regionMapping.get(currentRegion))
      .map(Object::toString)
      .map(bucketName -> Collections.singletonMap(currentRegion, bucketName))
      .orElse(Collections.singletonMap(primaryRegion, primaryBucket));
  }

  public static int getConfiguredExpirationInDays(final BlobStoreConfiguration blobStoreConfiguration) {
    return Integer.parseInt(
        blobStoreConfiguration.attributes(CONFIG_KEY).get(EXPIRATION_KEY, DEFAULT_EXPIRATION_IN_DAYS).toString()
    );
  }

  public static String getBucketPrefix(final BlobStoreConfiguration blobStoreConfiguration) {
    return Optional.ofNullable(blobStoreConfiguration.attributes(CONFIG_KEY).get(BUCKET_PREFIX_KEY, String.class))
        .filter(Predicates.not(Strings::isNullOrEmpty))
        .map(s -> s.replaceFirst("/$", "") + "/")
        .orElse("");
  }

  @VisibleForTesting
  static boolean regionLoaded;

  @VisibleForTesting
  static String region;

  @Nullable
  private static String getCurrentRegion() {
    if (!regionLoaded) {
      regionLoaded = true;
      try {
        region = Optional.ofNullable(Regions.getCurrentRegion())
            .map(Region::getName)
            .orElse(null);
      }
      catch (Exception e) {
        log.debug("Failed to retrieve region", e);
      }
    }
    log.trace("Current region {}", region);
    return region;
  }
}
