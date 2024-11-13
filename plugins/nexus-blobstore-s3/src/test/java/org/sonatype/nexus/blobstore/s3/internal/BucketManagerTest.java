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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.lifecycle.LifecycleAndOperator;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.s3.internal.BucketManager.LIFECYCLE_EXPIRATION_RULE_ID_PREFIX;
import static org.sonatype.nexus.blobstore.s3.internal.BucketManager.OLD_LIFECYCLE_EXPIRATION_RULE_ID;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.BUCKET_KEY;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.BUCKET_OWNERSHIP_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ERROR_CODE_MESSAGES;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INVALID_ACCESS_KEY_ID_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INVALID_IDENTITY_ERR_MSG;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.SIGNATURE_DOES_NOT_MATCH_CODE;

/**
 * {@link BucketManager} tests.
 */
@RunWith(JUnitParamsRunner.class)
public class BucketManagerTest
    extends TestSupport
{

  @Mock
  private AmazonS3 s3;

  @Mock
  private BucketOwnershipCheckFeatureFlag featureFlag;

  @Captor
  private ArgumentCaptor<BucketLifecycleConfiguration> lifeCycleCfgCaptor;

  @InjectMocks
  private BucketManager underTest;

  @Before
  public void setup() {
    when(featureFlag.isDisabled()).thenReturn(false);
  }

  @Test
  public void setLifeCycleOnExistingBucketIfNotPresent() {
    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "prefix", "myprefix", "expiration", "3"));
    cfg.setAttributes(attr);
    underTest.setS3(s3);

    underTest.prepareStorageLocation(cfg);

    verify(s3).getBucketPolicy("mybucket");
    verify(s3).setBucketLifecycleConfiguration(eq("mybucket"), notNull());
  }

  @Test
  public void isExpirationLifeCycleCfgPresentReturnsFalseOnEmptyConfig() {
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "prefix", "myprefix", "expiration", "3"));
    cfg.setAttributes(attr);
    BucketLifecycleConfiguration bucketCfg = new BucketLifecycleConfiguration();
    underTest.setS3(s3);

    assertFalse(underTest.isExpirationLifecycleConfigurationPresent(bucketCfg, cfg));
  }

  /**
   * Make sure if admins have set other lifecycle rules we don't clobber them.
   */
  @Test
  public void addingLifecycleRuleLeavesOtherRulesAlone() {
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration().withName("blobs");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "prefix", "myprefix", "expiration", "3"));
    cfg.setAttributes(attr);

    BucketLifecycleConfiguration bucketCfg = new BucketLifecycleConfiguration();

    Rule glacierRule = new BucketLifecycleConfiguration.Rule()
        .withId("some other rule")
        .withTransitions(ImmutableList.of(new Transition().withStorageClass(StorageClass.Glacier)
            .withDays(365)))
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    Rule otherBlobStoreRule = new BucketLifecycleConfiguration.Rule()
        .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "other")
        .withFilter(new LifecycleFilter(
            new LifecycleAndOperator(
                ImmutableList.of(
                    new LifecyclePrefixPredicate("otherPrefix/"),
                    new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)
                )
            )
        ))
        .withExpirationInDays(123)
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    bucketCfg.setRules(ImmutableList.of(glacierRule, otherBlobStoreRule));

    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(bucketCfg);

    underTest.prepareStorageLocation(cfg);

    verify(s3).setBucketLifecycleConfiguration(anyString(), lifeCycleCfgCaptor.capture());
    BucketLifecycleConfiguration capturedCfg = lifeCycleCfgCaptor.getValue();
    List<Rule> capturedRules = capturedCfg.getRules();
    assertEquals(3, capturedRules.size());
    assertTrue(capturedRules.stream().anyMatch(r -> "some other rule".equals(r.getId())));
    assertTrue(
        capturedRules.stream().anyMatch(r -> (LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "other").equals(r.getId())));
    assertTrue(
        capturedRules.stream().anyMatch(r -> (LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "blobs").equals(r.getId())));
  }

  @Test
  public void lifecycleRuleUpdatedWhenExpiryDateChanges() {
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration().withName("blobs");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "prefix", "myprefix", "expiration", "3"));
    cfg.setAttributes(attr);

    BucketLifecycleConfiguration bucketCfg = new BucketLifecycleConfiguration();

    Rule rule1 = new BucketLifecycleConfiguration.Rule()
        .withId("some other rule")
        .withTransitions(ImmutableList.of(new Transition().withStorageClass(StorageClass.Glacier)
            .withDays(365)))
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    Rule rule2 = new BucketLifecycleConfiguration.Rule()
        .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "blobs")
        .withFilter(new LifecycleFilter(
            new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)
        ))
        .withExpirationInDays(2)
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    bucketCfg.setRules(ImmutableList.of(rule1, rule2));

    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(bucketCfg);

    underTest.prepareStorageLocation(cfg);

    verify(s3).setBucketLifecycleConfiguration(anyString(), lifeCycleCfgCaptor.capture());
    BucketLifecycleConfiguration capturedCfg = lifeCycleCfgCaptor.getValue();
    List<Rule> capturedRules = capturedCfg.getRules();
    assertEquals(2, capturedRules.size());
    assertTrue(capturedRules.stream().anyMatch(r -> "some other rule".equals(r.getId())));
    assertTrue(capturedRules.stream().filter(r -> (LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "blobs").equals(r.getId()))
        .allMatch(r -> r.getExpirationInDays() == 3));
  }

  @Test
  public void lifecycleConfigurationRemovedIfAllRulesRemoved() {
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration().withName("mybucket");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "0"));
    cfg.setAttributes(attr);

    BucketLifecycleConfiguration bucketCfg = new BucketLifecycleConfiguration();

    Rule rule1 = new BucketLifecycleConfiguration.Rule()
        .withId(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "mybucket")
        .withFilter(new LifecycleFilter(
            new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)
        ))
        .withExpirationInDays(3)
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    bucketCfg.setRules(ImmutableList.of(rule1));

    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(bucketCfg);

    underTest.prepareStorageLocation(cfg);

    verify(s3).deleteBucketLifecycleConfiguration(anyString());
    verify(s3, times(0)).setBucketLifecycleConfiguration(anyString(), any());
  }

  @Test
  public void globalLifecycleRuleSwitchedToBlobStoreSpecificIfPresent() {
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration().withName("blobs");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "4"));
    cfg.setAttributes(attr);

    BucketLifecycleConfiguration bucketCfg = new BucketLifecycleConfiguration();

    Rule rule1 = new BucketLifecycleConfiguration.Rule()
        .withId(OLD_LIFECYCLE_EXPIRATION_RULE_ID)
        .withFilter(new LifecycleFilter(
            new LifecycleTagPredicate(S3BlobStore.DELETED_TAG)
        ))
        .withExpirationInDays(3)
        .withStatus(BucketLifecycleConfiguration.ENABLED);
    bucketCfg.setRules(ImmutableList.of(rule1));

    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(bucketCfg);

    underTest.prepareStorageLocation(cfg);

    verify(s3).setBucketLifecycleConfiguration(anyString(), lifeCycleCfgCaptor.capture());
    BucketLifecycleConfiguration capturedCfg = lifeCycleCfgCaptor.getValue();
    List<Rule> capturedRules = capturedCfg.getRules();
    assertEquals(1, capturedRules.size());
    assertEquals(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + "blobs", capturedRules.get(0).getId());
    assertEquals(4, capturedRules.get(0).getExpirationInDays());
  }

  @Test
  public void deleteStorageLocationRemovesBucketIfEmpty() {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(new ArrayList<>());
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3).deleteBucket("mybucket");
  }

  @Test
  public void deleteStorageLocationDoesNotRemoveBucketIfNotEmpty() {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(ImmutableList.of(new S3ObjectSummary()));
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(mock(BucketLifecycleConfiguration.class));
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3, times(0)).deleteBucket("mybucket");
  }

  @Test
  public void testOwnershipErrorIsNotThrownOnDisabledOwnershipCheck() {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    when(s3.getBucketPolicy(anyString())).thenThrow(AmazonClientException.class);
    when(featureFlag.isDisabled()).thenReturn(true);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    underTest.prepareStorageLocation(cfg);

    verify(s3, times(0)).getBucketPolicy(anyString());
  }

  @Test
  @Parameters(named = "ruleSetAndDeleteLifeCycleParams")
  public void itWillOnlyRemoveNxrmManagedLifeCyclesFromTheBucket(
      List<Rule> rules, int deleteLifeCycleCallCount, int setLifeCycleCallCount
  ) {
    ObjectListing listingMock = mock(ObjectListing.class);
    when(listingMock.getObjectSummaries()).thenReturn(ImmutableList.of(new S3ObjectSummary()));
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listingMock);
    BucketLifecycleConfiguration lifeCycleCfg = mock(BucketLifecycleConfiguration.class);
    when(lifeCycleCfg.getRules()).thenReturn(rules);
    when(s3.getBucketLifecycleConfiguration(anyString())).thenReturn(lifeCycleCfg);
    underTest.setS3(s3);

    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setName("my_s3_blob_store");
    Map<String, Map<String, Object>> attr = ImmutableMap.of("s3", ImmutableMap
        .of("bucket", "mybucket", "expiration", "3"));
    cfg.setAttributes(attr);

    underTest.deleteStorageLocation(cfg);

    verify(s3, times(deleteLifeCycleCallCount)).deleteBucketLifecycleConfiguration(anyString());
    verify(s3, times(setLifeCycleCallCount)).setBucketLifecycleConfiguration(anyString(),
        any(BucketLifecycleConfiguration.class));
  }

  @NamedParameters("ruleSetAndDeleteLifeCycleParams")
  private Object[] ruleSetAndDeleteLifeCycleParams() {
    return new Object[] {
        new Object[]{
            Collections.emptyList(), 1, 0
        },
        new Object[]{
            ImmutableList.of(oldNxrmRule()), 1, 0
        },
        new Object[]{
            ImmutableList.of(newNxrmRule("my_s3_blob_store")), 1, 0
        },
        new Object[]{
            ImmutableList.of(userRule()), 0, 1
        },
        new Object[]{
            ImmutableList.of(userRule(), oldNxrmRule(), userRule()), 0, 1
        },
        new Object[]{
            ImmutableList.of(userRule(), newNxrmRule("my_s3_blob_store"), userRule()), 0, 1
        }
    };
  }

  @Test
  @Parameters(named = "errorCodeAndMessageParams")
  public void errorThrownWhenBucketCannotBeCreated(
      String errorCode, String message
  ) {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(false);
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.createBucket(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageParams")
  private Object[] errorCodeAndMessageParams() {
    return new Object[] {
        new Object[]{
            ACCESS_DENIED_CODE, INSUFFICIENT_PERM_CREATE_BUCKET_ERR_MSG
        },
        new Object[]{
            "Some_Unexpected_Code", "An unexpected error occurred creating bucket. Check the logs for more details."
        }
    };
  }

  @Test
  @Parameters(named = "errorCodeAndMessageInvalidPermissionsParams")
  public void errorCodeAndMessageInvalidPermissionsParams(
      String errorCode, String message
  ) {
    String bucketName = "bucketName";
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.doesBucketExistV2(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageInvalidPermissionsParams")
  private Object[] errorCodeAndMessageInvalidPermissionsParams() {
    return new Object[] {
        new Object[]{
            "InvalidAccessKeyId", ERROR_CODE_MESSAGES.get(INVALID_ACCESS_KEY_ID_CODE)
        },
        new Object[]{
            "SignatureDoesNotMatch", ERROR_CODE_MESSAGES.get(SIGNATURE_DOES_NOT_MATCH_CODE)
        },
        new Object[]{
            "Some_Unexpected_Code", "An unexpected error occurred checking credentials. Check the logs for more details."
        }
    };
  }

  @Test
  @Parameters(named = "errorCodeAndMessageUserWithoutAccessParams")
  public void errorThrownIfUserDoesNotHaveAccessToAnExistingBucket(
      String errorCode, String message
  ) {
    String bucketName = "bucketName";
    when(s3.doesBucketExistV2(anyString())).thenReturn(true);
    AmazonS3Exception s3Exception = mock(AmazonS3Exception.class);
    when(s3Exception.getErrorCode()).thenReturn(errorCode);
    when(s3.getBucketPolicy(anyString())).thenThrow(s3Exception);

    Map<String, Map<String, Object>> cfgAttributes = ImmutableMap.of(CONFIG_KEY,
        ImmutableMap.of(BUCKET_KEY, bucketName));
    BlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(cfgAttributes);
    underTest.setS3(s3);

    Exception ex = assertThrows(S3BlobStoreException.class, () -> underTest.prepareStorageLocation(cfg));
    assertEquals(message, ex.getMessage());
  }

  @NamedParameters("errorCodeAndMessageUserWithoutAccessParams")
  private Object[] errorCodeAndMessageUserWithoutAccessParams() {
    return new Object[] {
        new Object[]{
            "AccessDenied", BUCKET_OWNERSHIP_ERR_MSG
        },
        new Object[]{
            "Some_Unexpected_Code", "An unexpected error occurred checking bucket ownership. Check the logs for more details."
        },
        new Object[]{
            "MethodNotAllowed", INVALID_IDENTITY_ERR_MSG
        }
    };
  }

  private Rule userRule() {
    Rule userRule = mock(Rule.class);
    when(userRule.getId()).thenReturn("user_rule_id");
    return userRule;
  }

  private Rule oldNxrmRule() {
    Rule oldRule = mock(Rule.class);
    when(oldRule.getId()).thenReturn(OLD_LIFECYCLE_EXPIRATION_RULE_ID);
    return oldRule;
  }

  private Rule newNxrmRule(String name) {
    Rule newRule = mock(Rule.class);
    when(newRule.getId()).thenReturn(LIFECYCLE_EXPIRATION_RULE_ID_PREFIX + name);
    return newRule;
  }

}
