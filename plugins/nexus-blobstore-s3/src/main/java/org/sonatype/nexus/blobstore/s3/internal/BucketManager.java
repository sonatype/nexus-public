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
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.lifecycle.LifecycleAndOperator;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilterPredicate;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getBucketPrefix;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreConfigurationHelper.getConfiguredExpirationInDays;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.*;

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
  static final String OLD_LIFECYCLE_EXPIRATION_RULE_ID = "Expire soft-deleted blobstore objects";

  static final String LIFECYCLE_EXPIRATION_RULE_ID_PREFIX = "Expire soft-deleted objects in blobstore ";

  private AmazonS3 s3;

  public void setS3(final AmazonS3 s3) {
    this.s3 = s3;
  }

  @Override
  public void prepareStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    checkPermissions(getConfiguredBucket(blobStoreConfiguration));
    if (!s3.doesBucketExistV2(bucket)) {
      try {
        s3.createBucket(bucket);
      }
      catch (AmazonS3Exception e) {
        if (ACCESS_DENIED_CODE.equals(e.getErrorCode())) {
          log.debug("Error creating bucket {}", bucket, e);
          throw insufficientCreatePermissionsError();
        }
        log.info("Error creating bucket {}", bucket, e);
        throw unexpectedError("creating bucket");
      }
      setBucketLifecycleConfiguration(s3, blobStoreConfiguration, null);
    }
    else {
      // bucket exists, we should test that the correct lifecycle config is present
      try {
        BucketLifecycleConfiguration lifecycleConfiguration = s3.getBucketLifecycleConfiguration(bucket);
        if (!isExpirationLifecycleConfigurationPresent(lifecycleConfiguration, blobStoreConfiguration)) {
          setBucketLifecycleConfiguration(s3, blobStoreConfiguration, lifecycleConfiguration);
        }
      } catch (AmazonS3Exception e) {
        if (NOT_IMPLEMENTED_CODE.equals(e.getErrorCode())) {
          log.warn("Bucket {} does not support managing lifecycle config.", bucket, e);
        } else {
          throw e;
        }
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
      try {
        BucketLifecycleConfiguration lifecycleConfiguration = s3.getBucketLifecycleConfiguration(bucket);
        List<Rule> nonBlobstoreRules = nonBlobstoreRules(lifecycleConfiguration, blobStoreConfiguration.getName());
        if(!isEmpty(nonBlobstoreRules)) {
          lifecycleConfiguration.setRules(nonBlobstoreRules);
          s3.setBucketLifecycleConfiguration(bucket, lifecycleConfiguration);
        } else {
          s3.deleteBucketLifecycleConfiguration(bucket);
        }
      } catch (AmazonS3Exception e) {
        if (NOT_IMPLEMENTED_CODE.equals(e.getErrorCode())) {
          log.warn("Bucket {} does not support managing lifecycle config.", bucket, e);
        } else {
          throw e;
        }
      }
    }
  }

  private boolean isExpirationLifecycleConfigurationPresent(final BucketLifecycleConfiguration lifecycleConfiguration,
                                                            final BlobStoreConfiguration blobStoreConfiguration) {
    String bucketPrefix = getBucketPrefix(blobStoreConfiguration);
    int expirationInDays = getConfiguredExpirationInDays(blobStoreConfiguration);
    return lifecycleConfiguration != null &&
        lifecycleConfiguration.getRules() != null &&
        lifecycleConfiguration.getRules().stream()
        .filter(r -> r.getExpirationInDays() == expirationInDays)
        .anyMatch(r -> isDeletedTagPredicate(r.getFilter().getPredicate(), bucketPrefix));
  }

  private BucketLifecycleConfiguration makeLifecycleConfiguration(final BucketLifecycleConfiguration existing,
                                                                  final BlobStoreConfiguration blobStoreConfiguration) {
    String blobStoreName = blobStoreConfiguration.getName();
    String bucketPrefix = getBucketPrefix(blobStoreConfiguration);
    int expirationInDays = getConfiguredExpirationInDays(blobStoreConfiguration);
    LifecycleFilterPredicate filterPredicate;
    if (bucketPrefix.isEmpty()) {
      filterPredicate = new LifecycleTagPredicate(S3BlobStore.DELETED_TAG);
    }
    else {
      filterPredicate = new LifecycleAndOperator(asList(
        new LifecyclePrefixPredicate(bucketPrefix),
        new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)));
    }
    BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
        .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + blobStoreName)
        .withFilter(new LifecycleFilter(filterPredicate))
        .withExpirationInDays(expirationInDays)
        .withStatus(BucketLifecycleConfiguration.ENABLED);

    BucketLifecycleConfiguration newConfiguration = null;
    if (existing != null && !existing.getRules().isEmpty()) {
      List<Rule> rules = nonBlobstoreRules(existing, blobStoreName);
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

  private List<Rule> nonBlobstoreRules(final BucketLifecycleConfiguration existing, final String blobStoreName) {
    List<Rule> rules = existing.getRules();
    if (rules == null) {
      return emptyList();
    }
    return rules.stream()
        .filter(r -> !r.getId().equals(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + blobStoreName) &&
                !r.getId().equals(OLD_LIFECYCLE_EXPIRATION_RULE_ID))
        .collect(toList());
  }

  private void setBucketLifecycleConfiguration(final AmazonS3 s3,
                                               final BlobStoreConfiguration blobStoreConfiguration,
                                               final BucketLifecycleConfiguration lifecycleConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    BucketLifecycleConfiguration newLifecycleConfiguration =
        makeLifecycleConfiguration(lifecycleConfiguration, blobStoreConfiguration);
    try {
      if (newLifecycleConfiguration != null) {
        s3.setBucketLifecycleConfiguration(bucket, newLifecycleConfiguration);
      }
      else if (lifecycleConfiguration != null && !lifecycleConfiguration.getRules().isEmpty()) {
        s3.deleteBucketLifecycleConfiguration(bucket);
      }
    } catch (AmazonS3Exception e) {
      if (NOT_IMPLEMENTED_CODE.equals(e.getErrorCode())) {
        log.warn("Bucket {} does not support managing lifecycle config.", bucket, e);
      } else {
        throw e;
      }
    }
  }

  private boolean isDeletedTagPredicate(final LifecycleFilterPredicate filterPredicate, final String bucketPrefix) {
    if (filterPredicate instanceof LifecycleTagPredicate) {
      LifecycleTagPredicate tagPredicate = (LifecycleTagPredicate) filterPredicate;
      return S3BlobStore.DELETED_TAG.equals(tagPredicate.getTag());
    }
    else if (filterPredicate instanceof LifecycleAndOperator) {
      LifecycleAndOperator andOperator = (LifecycleAndOperator) filterPredicate;
      return
          andOperator.getOperands().stream().anyMatch(op -> isDeletedTagPredicate(op, bucketPrefix)) &&
          andOperator.getOperands().stream().anyMatch(op -> isBucketPrefixPredicate(op, bucketPrefix));
    }
    else {
      return false;
    }
  }

  private boolean isBucketPrefixPredicate(final LifecycleFilterPredicate filterPredicate, final String bucketPrefix) {
    if (filterPredicate instanceof LifecyclePrefixPredicate) {
      LifecyclePrefixPredicate prefixPredicate = (LifecyclePrefixPredicate) filterPredicate;
      return prefixPredicate.getPrefix().equals(bucketPrefix);
    }
    else {
      return false;
    }
  }

  private void checkPermissions(final String bucket) {
    checkCredentials(bucket);
    if (s3.doesBucketExistV2(bucket)) {
      checkBucketOwner(bucket);
    }
  }

  private void checkCredentials(final String bucket) {
    try {
      s3.doesBucketExistV2(bucket);
    }
    catch (AmazonS3Exception e) {
      if (INVALID_ACCESS_KEY_ID_CODE.equals(e.getErrorCode()) ||
          SIGNATURE_DOES_NOT_MATCH_CODE.equals(e.getErrorCode())) {
        log.debug("Exception thrown checking AWS credentials", e);
        throw buildException(e);
      }
      log.info("Exception thrown checking AWS credentials.", e);
      throw unexpectedError("checking credentials");
    }
  }

  private void checkBucketOwner(final String bucket) {
    try {
      s3.getBucketAcl(bucket);
    }
    catch (AmazonS3Exception e) {
      if (ACCESS_DENIED_CODE.equals(e.getErrorCode())) {
        log.debug("Exception thrown checking bucket owner.", e);
        throw bucketOwnershipError();
      }
      log.info("Exception thrown checking bucket owner.", e);
      throw unexpectedError("checking bucket ownership");
    }
  }
}
