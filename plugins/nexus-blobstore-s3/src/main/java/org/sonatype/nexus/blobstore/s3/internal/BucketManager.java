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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.List;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.StorageLocationManager;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilterPredicate;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;

import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredExpirationInDays;

/**
 * Creates and deletes buckets for the {@link S3BlobStore}.
 *
 * @since 3.16
 */
@Named
public class BucketManager
    extends ComponentSupport
    implements StorageLocationManager
{
  private static final String LIFECYCLE_EXPIRATION_RULE_ID = "Expire soft-deleted blobstore objects";

  private AmazonS3 s3;

  public void setS3(final AmazonS3 s3) {
    this.s3 = s3;
  }

  @Override
  public void prepareStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    int expirationInDays = getConfiguredExpirationInDays(blobStoreConfiguration);

    if (!s3.doesBucketExistV2(bucket)) {
      s3.createBucket(bucket);
      setBucketLifecycleConfiguration(s3, bucket, null, expirationInDays);
    }
    else {
      // bucket exists, we should test that the correct lifecycle config is present
      BucketLifecycleConfiguration lifecycleConfiguration = s3.getBucketLifecycleConfiguration(bucket);
      if (!isExpirationLifecycleConfigurationPresent(lifecycleConfiguration, expirationInDays)) {
        setBucketLifecycleConfiguration(s3, bucket, lifecycleConfiguration, expirationInDays);
      }
    }
  }

  @Override
  public void deleteStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    ObjectListing listing = s3.listObjects(new ListObjectsRequest().withBucketName(bucket).withMaxKeys(1));
    if (listing.getObjectSummaries().isEmpty()) {
      s3.deleteBucket(bucket);
    }
    else {
      log.info("Not removing S3 bucket {} because it is not empty", bucket);
    }
  }

  private boolean isExpirationLifecycleConfigurationPresent(final BucketLifecycleConfiguration lifecycleConfiguration,
                                                            final int expirationInDays) {
    return lifecycleConfiguration != null &&
        lifecycleConfiguration.getRules() != null &&
        lifecycleConfiguration.getRules().stream()
        .filter(r -> r.getExpirationInDays() == expirationInDays)
        .anyMatch(r -> {
          LifecycleFilterPredicate predicate = r.getFilter().getPredicate();
          if (predicate instanceof LifecycleTagPredicate) {
            LifecycleTagPredicate tagPredicate = (LifecycleTagPredicate) predicate;
            return S3BlobStore.DELETED_TAG.equals(tagPredicate.getTag());
          }
          return false;
        });
  }

  private BucketLifecycleConfiguration makeLifecycleConfiguration(final BucketLifecycleConfiguration existing,
                                                                  final int expirationInDays) {
    BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
        .withId(LIFECYCLE_EXPIRATION_RULE_ID)
        .withFilter(new LifecycleFilter(
            new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)))
        .withExpirationInDays(expirationInDays)
        .withStatus(BucketLifecycleConfiguration.ENABLED);

    BucketLifecycleConfiguration newConfiguration = null;
    if (existing != null) {
      List<Rule> rules = existing.getRules().stream()
          .filter(r -> !LIFECYCLE_EXPIRATION_RULE_ID.equals(r.getId()))
          .collect(toList());
      if (expirationInDays > 0) {
        rules.add(rule);
      }
      if (!rules.isEmpty()) {
        existing.setRules(rules);
        newConfiguration = existing;
      }
    }
    else {
      if (expirationInDays > 0) {
        newConfiguration = new BucketLifecycleConfiguration().withRules(rule);
      }
    }
    return newConfiguration;
  }

  private void setBucketLifecycleConfiguration(final AmazonS3 s3,
                                               final String bucket,
                                               final BucketLifecycleConfiguration lifecycleConfiguration,
                                               final int expirationInDays) {
    BucketLifecycleConfiguration newLifecycleConfiguration =
        makeLifecycleConfiguration(lifecycleConfiguration, expirationInDays);
    if (newLifecycleConfiguration != null) {
      s3.setBucketLifecycleConfiguration(bucket, newLifecycleConfiguration);
    }
    else {
      s3.deleteBucketLifecycleConfiguration(bucket);
    }
  }
}
