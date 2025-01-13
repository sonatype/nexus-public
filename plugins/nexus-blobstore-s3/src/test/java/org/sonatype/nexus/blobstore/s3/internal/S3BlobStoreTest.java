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

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.VolumeChapterLocationStrategy;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.blobstore.s3.internal.datastore.DatastoreS3BlobStoreMetricsService;
import org.sonatype.nexus.common.log.DryRunPrefix;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.DeleteObjectsResult.DeletedObject;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_FILE_ATTRIBUTES_SUFFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_FILE_CONTENT_SUFFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;

public class S3BlobStoreTest
    extends TestSupport
{

  @Mock
  private AmazonS3Factory amazonS3Factory;

  @Mock
  private S3Uploader uploader;

  @Mock
  private S3Copier copier;

  @Mock
  private DatastoreS3BlobStoreMetricsService storeMetrics;

  @Mock
  private BlobStoreQuotaUsageChecker blobStoreQuotaUsageChecker;

  @Mock
  private DryRunPrefix dryRunPrefix;

  @Mock
  private BucketManager bucketManager;

  @Mock
  private AmazonS3 s3;

  private MockedStatic<Regions> regionsMockedStatic;

  private S3BlobStore blobStore;

  private MockBlobStoreConfiguration config;

  private String attributesContents;

  @Before
  public void setUp() {
    regionsMockedStatic = mockStatic(Regions.class);
    Region region = mock(Region.class);
    when(region.getName()).thenReturn("us-east-1");
    regionsMockedStatic.when(Regions::getCurrentRegion).thenReturn(region);
    blobStore = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(true), uploader, copier, false,
        false, false, storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker);
    config = new MockBlobStoreConfiguration();
    attributesContents =
        "#Thu Jun 01 23:10:55 UTC 2017\n@BlobStore.created-by=admin\nsize=11\n@Bucket.repo-name=test\ncreationTime=1496358655289\n@BlobStore.content-type=text/plain\n@BlobStore.blob-name=test\nsha1=eb4c2a5a1c04ca2d504c5e57e1f88cef08c75707";
    when(amazonS3Factory.create(any())).thenReturn(s3);
    config
        .setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
  }

  @After
  public void teardown() {
    regionsMockedStatic.close();
  }

  @Test
  public void testGetBlobIdStreamWorksWithPrefix() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    blobStore.init(cfg);
    blobStore.doStart();

    when(s3.listObjects(ArgumentMatchers.any(ListObjectsRequest.class))).thenAnswer(invocation -> {
      ListObjectsRequest request = invocation.getArgument(0);
      assertThat(request.getPrefix(), is("myPrefix/content/"));

      ObjectListing listing = new ObjectListing();
      S3ObjectSummary summary1 = new S3ObjectSummary();
      summary1.setBucketName("mybucket");
      summary1.setKey("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties");
      S3ObjectSummary summary2 = new S3ObjectSummary();
      summary2.setBucketName("mybucket");
      summary2.setKey("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes");
      listing.getObjectSummaries().add(summary1);
      listing.getObjectSummaries().add(summary2);
      listing.setTruncated(false);
      return listing;
    });

    when(s3.getObjectMetadata(anyString(), anyString())).thenReturn(new ObjectMetadata());

    List<BlobId> blobIdStream = blobStore.getBlobIdStream().toList();
    assertThat(blobIdStream.size(), is(1));
  }

  @Test
  public void testGetBlobIdUpdatedSinceStreamFiltersOutOfDateContent() throws Exception {
    blobStore.init(config);
    blobStore.doStart();

    when(s3.listObjects(ArgumentMatchers.any(ListObjectsRequest.class))).thenAnswer(invocation -> {
      ObjectListing listing = new ObjectListing();

      S3ObjectSummary summary1 = new S3ObjectSummary();
      summary1.setBucketName("mybucket");
      summary1.setKey("/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.properties");
      summary1.setLastModified(new Date());
      listing.getObjectSummaries().add(summary1);

      S3ObjectSummary summary2 = new S3ObjectSummary();
      summary2.setBucketName("mybucket");
      summary2.setKey("/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.bytes");
      summary2.setLastModified(new Date());
      listing.getObjectSummaries().add(summary2);

      S3ObjectSummary summary3 = new S3ObjectSummary();
      summary3.setBucketName("mybucket");
      summary3.setKey("vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties");
      summary3.setLastModified(new Date());
      listing.getObjectSummaries().add(summary3);

      S3ObjectSummary summary4 = new S3ObjectSummary();
      summary4.setBucketName("mybucket");
      summary4.setKey("vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes");
      summary4.setLastModified(new Date());
      listing.getObjectSummaries().add(summary4);

      S3ObjectSummary summary5 = new S3ObjectSummary();
      summary5.setBucketName("mybucket");
      summary5.setKey("vol-01/chap-01/12345678-1234-1234-1234-123456789def.properties");
      summary5.setLastModified(new Date(System.currentTimeMillis() - 2));
      listing.getObjectSummaries().add(summary5);

      S3ObjectSummary summary6 = new S3ObjectSummary();
      summary6.setBucketName("mybucket");
      summary6.setKey("vol-01/chap-01/12345678-1234-1234-1234-123456789def.bytes");
      summary6.setLastModified(new Date(System.currentTimeMillis() - 2));
      listing.getObjectSummaries().add(summary6);
      return listing;
    });

    when(s3.getObjectMetadata("mybucket", "/content/vol-01/chap-01/12345678-1234-1234-1234-123456789ghi.properties"))
        .thenReturn(getTempBlobMetadata());
    when(s3.getObjectMetadata("mybucket", "vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties"))
        .thenReturn(new ObjectMetadata());

    List<BlobId> blobIds = blobStore.getBlobIdUpdatedSinceStream(Duration.ofDays(1L)).toList();
    assertThat(blobIds.size(), is(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetBlobIdUpdatedSinceStreamThrowsExceptionIfNegativeSinceDaysIsPassedIn() throws Exception {
    blobStore.init(config);
    blobStore.doStart();
    blobStore.getBlobIdUpdatedSinceStream(Duration.ofDays(-1L));
  }

  @Test
  public void testGetBlobWithBucketPrefix() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "prefix")))));

    BlobId blobId = new BlobId("test");
    S3Object attributesS3Object = mockS3Object(attributesContents);
    S3Object contentS3Object = mockS3Object("hello world");

    doNothing().when(bucketManager).prepareStorageLocation(cfg);
    when(s3.doesObjectExist("mybucket", "prefix/metadata.properties")).thenReturn(false);
    when(s3.getObject("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);
    when(s3.getObject("mybucket", "prefix/" + bytesLocation(blobId))).thenReturn(contentS3Object);

    blobStore.init(cfg);
    blobStore.doStart();
    Blob blob = blobStore.get(blobId);

    assertThat(blob, notNullValue());
    String content = new String(blob.getInputStream().readAllBytes());

    assertThat(content, is("hello world"));

    verify(bucketManager).prepareStorageLocation(cfg);
    verify(s3).doesObjectExist("mybucket", "prefix/metadata.properties");
    verify(s3).getObject("mybucket", "prefix/" + propertiesLocation(blobId));
    verify(s3).getObject("mybucket", "prefix/" + bytesLocation(blobId));
  }

  @Test
  public void testSoftDeleteSuccessfulWithBucketPrefix() throws Exception {
    BlobId blobId = new BlobId("soft-delete-success");
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "prefix")))));
    blobStore.init(cfg);
    blobStore.doStart();
    when(s3.doesObjectExist("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(true);
    S3Object attributesS3Object = mockS3Object(attributesContents);
    when(s3.getObject("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);
    boolean deleted = blobStore.delete(blobId, "successful test");
    ArgumentCaptor<SetObjectTaggingRequest> objectTaggingRequestArgumentCaptor =
        ArgumentCaptor.forClass(SetObjectTaggingRequest.class);
    verify(s3, times(2)).setObjectTagging(objectTaggingRequestArgumentCaptor.capture());
    List<SetObjectTaggingRequest> capturedRequests = objectTaggingRequestArgumentCaptor.getAllValues();

    assertTrue(capturedRequests.get(0).getKey().endsWith(BLOB_FILE_CONTENT_SUFFIX));
    assertThat(capturedRequests.get(0).getTagging().getTagSet(), hasItem(S3BlobStore.DELETED_TAG));

    assertTrue(capturedRequests.get(1).getKey().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX));
    assertThat(capturedRequests.get(1).getTagging().getTagSet(), hasItem(S3BlobStore.DELETED_TAG));
    assertThat(deleted, is(true));
  }

  @Test
  public void testSoftDeleteReturnsFalseWhenBlobDoesNotExist() throws Exception {
    blobStore.init(config);
    blobStore.doStart();
    mockPropertiesException();
    boolean deleted = blobStore.delete(new BlobId("soft-delete-fail"), "test");
    assertThat(deleted, is(false));
    verify(s3, never()).setObjectTagging(any());
  }

  @Test
  public void testDeleteIsHardWhenExpiryDaysIsZero() throws Exception {
    BlobId blobId = new BlobId("some-blob");
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "")))));
    blobStore.init(cfg);
    blobStore.doStart();
    S3Object attributesS3Object = mockS3Object(attributesContents);
    when(s3.doesObjectExist("mybucket", propertiesLocation(blobId))).thenReturn(true);
    when(s3.getObject("mybucket", propertiesLocation(blobId))).thenReturn(attributesS3Object);

    DeleteObjectsResult deleteObjectsResult = mock(DeleteObjectsResult.class);
    when(deleteObjectsResult.getDeletedObjects()).thenReturn(List.of(new DeletedObject(), new DeletedObject()));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResult);

    cfg.attributes("s3").set("expiration", 0);
    blobStore.delete(blobId, "just a test");
    verify(s3).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  public void testUndeleteSuccessful() throws Exception {
    Properties properties = new Properties();
    properties.put("@BlobStore.blob-name", "my-blob");
    S3BlobAttributes blobAttributes = mock(S3BlobAttributes.class);
    when(blobAttributes.getProperties()).thenReturn(properties);
    when(blobAttributes.isDeleted()).thenReturn(true);
    BlobStoreUsageChecker usageChecker = mock(BlobStoreUsageChecker.class);
    when(usageChecker.test(any(), any(), any())).thenReturn(true);
    blobStore.init(config);
    blobStore.doStart();

    boolean restored = blobStore.undelete(usageChecker, new BlobId("restore-succeed"), blobAttributes, true);
    assertThat(restored, is(true));
    verify(s3, never()).setObjectTagging(any());

    when(blobAttributes.getMetrics()).thenReturn(mock(BlobMetrics.class));
    restored = blobStore.undelete(usageChecker, new BlobId("restore-succeed"), blobAttributes, false);
    assertThat(restored, is(true));
    verify(blobAttributes).setDeleted(false);
    verify(blobAttributes).setDeletedReason(null);

    ArgumentCaptor<SetObjectTaggingRequest> objectTaggingRequestArgumentCaptor =
        ArgumentCaptor.forClass(SetObjectTaggingRequest.class);
    verify(s3, times(2)).setObjectTagging(objectTaggingRequestArgumentCaptor.capture());
    List<SetObjectTaggingRequest> capturedRequests = objectTaggingRequestArgumentCaptor.getAllValues();

    assertTrue(capturedRequests.get(0).getKey().endsWith(BLOB_FILE_CONTENT_SUFFIX));
    assertTrue(capturedRequests.get(0).getTagging().getTagSet().isEmpty());

    assertTrue(capturedRequests.get(1).getKey().endsWith(BLOB_FILE_ATTRIBUTES_SUFFIX));
    assertTrue(capturedRequests.get(1).getTagging().getTagSet().isEmpty());
  }

  @Test
  public void testStartWillAcceptMetadataPropertiesOriginallyCreatedWithFileBlobstore() throws Exception {
    when(s3.doesObjectExist("mybucket","myPrefix/metadata.properties")).thenReturn(true);
    S3Object s3Object = mockS3Object("type=file/1");
    when(s3.getObject("mybucket", "myPrefix/metadata.properties")).thenReturn(s3Object);
    blobStore.init(config);
    blobStore.doStart();
    verify(amazonS3Factory).create(any());
  }

  @Test
  public void testStartRejectsMetadataPropertiesContainingSomethingOtherThanFileOrS3Type() {
    when(s3.doesObjectExist(anyString(), anyString())).thenReturn(true);
    S3Object s3Object = mockS3Object("type=other/12");
    when(s3.getObject(anyString(), anyString())).thenReturn(s3Object);
    blobStore.init(config);
    assertThrows(IllegalStateException.class, () -> blobStore.doStart());
  }

  @Test
  public void testRemoveBucketErrorThrowsException() throws Exception {
    when(s3.listObjects("mybucket", "myPrefix/content/")).thenReturn(new ObjectListing());
    blobStore.init(config);
    blobStore.doStart();
    AmazonS3Exception s3Exception = new AmazonS3Exception("error");
    s3Exception.setErrorCode("UnknownError");
    doThrow(s3Exception).when(bucketManager).deleteStorageLocation(config);
    assertThrows(BlobStoreException.class, () -> blobStore.remove());
    verify(storeMetrics).remove();
    verify(s3).deleteObject("mybucket", "myPrefix/metadata.properties");
  }

  @Test
  public void testRemoveNonEmptyBucketGeneratesWarningOnly() throws Exception {
    when(s3.listObjects("mybucket", "myPrefix/content/")).thenReturn(new ObjectListing());
    blobStore.init(config);
    blobStore.doStart();
    AmazonS3Exception s3Exception = new AmazonS3Exception("error");
    s3Exception.setErrorCode("BucketNotEmpty");
    doThrow(s3Exception).when(bucketManager).deleteStorageLocation(any());
    blobStore.remove();
    verify(storeMetrics).remove();
    verify(s3).deleteObject("mybucket", "myPrefix/metadata.properties");
  }

  @Test
  public void testRemovingNonEmptyBlobStoreRemovesLifecyclePolicy() throws Exception {
    ObjectListing objectListing = mock(ObjectListing.class);
    when(objectListing.getObjectSummaries()).thenReturn(List.of(new S3ObjectSummary()));
    when(s3.listObjects("mybucket", "myPrefix/content/")).thenReturn(objectListing);
    blobStore.init(config);
    blobStore.doStart();
    blobStore.remove();
    verify(s3, never()).deleteObject("mybucket", "myPrefix/metadata.properties");
    verify(bucketManager, never()).deleteStorageLocation(config);
    verify(s3).deleteBucketLifecycleConfiguration("mybucket");
  }

  @Test
  public void testBucketNameRegexValidates() {
    assertThat("".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("ab".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("abc".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("0123456789".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890".matches(S3BlobStore.BUCKET_REGEX),
        is(true));
    assertThat("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz012345678901".matches(S3BlobStore.BUCKET_REGEX),
        is(false));
    assertThat("foo.bar".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("foo-bar".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("foo.bar-blat".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("foo..bar".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat(".foobar".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("foo.-bar".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("foo-.bar".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("foobar-".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("foobar.".matches(S3BlobStore.BUCKET_REGEX), is(false));
    assertThat("01234.56789".matches(S3BlobStore.BUCKET_REGEX), is(true));
    assertThat("127.0.0.1".matches(S3BlobStore.BUCKET_REGEX), is(false));
  }

  @Test
  public void testCreateDirectPathBlob() throws Exception {
    String expectedBytesPath = "myPrefix/content/directpath/foo/bar/myblob.bytes";
    String expectedPropertiesPath = "myPrefix/content/directpath/foo/bar/myblob.properties";
    blobStore.init(config);
    blobStore.doStart();

    mockPropertiesException();
    BlobId blobId = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), Map.of("BlobStore.direct-path",
        "true", "BlobStore.blob-name", "foo/bar/myblob", "BlobStore.created-by", "test")).getId();

    verify(s3).putObject(eq("mybucket"), eq(expectedPropertiesPath), any(), any());
    verify(uploader).upload(any(), eq("mybucket"), eq(expectedBytesPath), any());

    ObjectListing listing = new ObjectListing();
    S3ObjectSummary summary1 = new S3ObjectSummary();
    summary1.setBucketName("mybucket");
    summary1.setKey(expectedPropertiesPath);
    listing.getObjectSummaries().add(summary1);
    S3ObjectSummary summary2 = new S3ObjectSummary();
    summary2.setBucketName("mybucket");
    summary2.setKey(expectedBytesPath);
    listing.getObjectSummaries().add(summary2);
    when(s3.listObjects(any(ListObjectsRequest.class))).thenReturn(listing);

    List<BlobId> blobIdStream = blobStore.getDirectPathBlobIdStream("foo/bar").toList();
    assertThat(blobIdStream, is(List.of(blobId)));
  }

  @Test
  public void testS3BlobStoreIsWritableWhenClientCanVerifyBucketExists() throws Exception {
    when(s3.doesBucketExistV2("mybucket")).thenReturn(true);
    blobStore.init(config);
    blobStore.doStart();
    assertThat(blobStore.isStorageAvailable(), is(true));

    when(s3.doesBucketExistV2("mybucket")).thenReturn(false);
    assertThat(blobStore.isStorageAvailable(), is(false));

    when(s3.doesBucketExistV2("mybucket")).thenThrow(new SdkClientException("Fake error"));
    assertThat(blobStore.isStorageAvailable(), is(false));
  }

  @Test
  public void testExpiry() throws Exception {
    S3BlobStore expiryPreferredBlobStore = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(true),
        uploader, copier, true, false, false, storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker);
    BlobId blobId = new BlobId("soft-delete-success");
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    expiryPreferredBlobStore.init(cfg);
    expiryPreferredBlobStore.doStart();

    when(s3.doesObjectExist("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(true);
    S3Object attributesS3Object = mockS3Object(attributesContents);
    when(s3.getObject("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);

    boolean deleted = expiryPreferredBlobStore.deleteHard(blobId);
    assertThat(deleted, is(true));
    verify(s3, never()).deleteObject(anyString(), anyString());
  }

  @Test
  public void testHardDeleteHardDeletesWhenPreferred() throws Exception {
    S3BlobStore hardDeleteStore = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(true), uploader,
        copier, true, true, false, storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker);
    BlobId blobId = new BlobId("soft-delete-success");
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    hardDeleteStore.init(cfg);
    hardDeleteStore.doStart();

    when(s3.doesObjectExist("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(true);
    S3Object attributesS3Object = mockS3Object(attributesContents);
    when(s3.getObject("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);

    DeleteObjectsResult deleteObjectsResult = mock(DeleteObjectsResult.class);
    when(deleteObjectsResult.getDeletedObjects()).thenReturn(List.of(new DeletedObject(), new DeletedObject()));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResult);

    boolean deleted = hardDeleteStore.deleteHard(blobId);
    assertThat(deleted, is(true));
    verify(s3).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  public void testRegularDeleteHardDeletesWhenPreferred() throws Exception {
    S3BlobStore hardDeleteStore = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(true), uploader,
        copier, true, true, false, storeMetrics, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker);
    BlobId blobId = new BlobId("soft-delete-success");
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    hardDeleteStore.init(cfg);
    hardDeleteStore.doStart();

    when(s3.doesObjectExist("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(true);
    S3Object attributesS3Object = mockS3Object(attributesContents);
    when(s3.getObject("mybucket", "myPrefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);

    DeleteObjectsResult deleteObjectsResult = mock(DeleteObjectsResult.class);
    when(deleteObjectsResult.getDeletedObjects()).thenReturn(List.of(new DeletedObject(), new DeletedObject()));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResult);

    boolean deleted = hardDeleteStore.delete(blobId, "testDelete");
    assertThat(deleted, is(true));
    verify(s3).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  public void testConcurrentAttemptsToRefreshBlobShouldNeverReturnNull() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket")))));
    BlobId blobId = new BlobId("test");
    when(s3.doesObjectExist("mybucket", propertiesLocation(blobId))).thenReturn(true);
    S3Object attributesS3Object = mockS3Object(attributesContents);
    S3Object contentS3Object = mockS3Object("hello world");
    when(s3.getObject("mybucket", propertiesLocation(blobId))).thenReturn(attributesS3Object);
    when(s3.getObject("mybucket", bytesLocation(blobId))).thenReturn(contentS3Object);

    blobStore.init(cfg);
    blobStore.doStart();

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Callable<Blob> callable = () -> blobStore.get(blobId);
    List<Future<Blob>> results = List.of(executorService.submit(callable), executorService.submit(callable));

    executorService.shutdown();
    assertThat(results.get(0).get(), is(notNullValue()));
    assertThat(results.get(1).get(), is(notNullValue()));
  }

  @Test
  public void testCreateDoesNotCreateTempBlobsWithTmpBlobId() throws Exception {
    blobStore.init(config);
    blobStore.doStart();

    Map<String, String> headers = new HashMap<>(Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1",
        BLOB_NAME_HEADER, "temp", TEMPORARY_BLOB_HEADER, ""));
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob.getId().asUniqueString().startsWith("tmp$"), is(false));
    assertThat(blob.getHeaders(), is(headers));

    headers.remove(TEMPORARY_BLOB_HEADER);
    headers.putAll(
        Map.of(BLOB_NAME_HEADER, "file.txt", CONTENT_TYPE_HEADER, "text/plain", REPO_NAME_HEADER, "a repository"));
    blob = blobStore.makeBlobPermanent(blob.getId(), headers);

    ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
    verify(s3, times(3)).putObject(eq("mybucket"), anyString(), any(), metadataCaptor.capture());
    List<ObjectMetadata> metadataList = metadataCaptor.getAllValues();
    assertThat(metadataList.get(1).getUserMetadata(), hasEntry(TEMPORARY_BLOB_HEADER, "true"));

    assertThat(blob.getHeaders(), is(headers));
    assertThat(metadataList.get(2).getUserMetadata(), not(hasKey(TEMPORARY_BLOB_HEADER)));
  }

  @Test
  public void testMakeBlobPermanentThrowsExceptionIfTempBlobHeaderIsPassedIn() throws Exception {
    blobStore.init(config);
    blobStore.doStart();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "temp",
            TEMPORARY_BLOB_HEADER, "");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThrows(IllegalArgumentException.class, () -> blobStore.makeBlobPermanent(blob.getId(), headers)); // NOSONAR
  }

  @Test
  public void testDeleteIfTempDeletesBlobWhenTempBlobHeaderIsPresent() throws Exception {
    blobStore.init(config);
    blobStore.doStart();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "temp",
            TEMPORARY_BLOB_HEADER, "");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob, is(notNullValue()));

    DeleteObjectsResult deleteObjectsResult = mock(DeleteObjectsResult.class);
    when(deleteObjectsResult.getDeletedObjects()).thenReturn(List.of(new DeletedObject(), new DeletedObject()));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResult);

    boolean deleted = blobStore.deleteIfTemp(blob.getId());
    assertThat(deleted, is(true));

    mockPropertiesException();
    Blob retrievedBlob = blobStore.get(blob.getId());
    assertThat(retrievedBlob, is(nullValue()));
    verify(s3).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  public void testDeleteIfTempDoesNotDeleteBlobWhenTempBlobHeaderIsAbsent() throws Exception {
    blobStore.init(config);
    blobStore.doStart();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "file.txt",
            CONTENT_TYPE_HEADER, "text/plain", REPO_NAME_HEADER, "a repository");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob, is(notNullValue()));

    boolean deleted = blobStore.deleteIfTemp(blob.getId());
    assertThat(deleted, is(false));
    Blob retrievedBlob = blobStore.get(blob.getId());
    assertThat(retrievedBlob, is(notNullValue()));
    verify(s3, never()).deleteObjects(any());
  }

  private String propertiesLocation(BlobId blobId) {
    return "content/" + new VolumeChapterLocationStrategy().location(blobId) + ".properties";
  }

  private String bytesLocation(BlobId blobId) {
    return "content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";
  }

  private static ObjectMetadata getTempBlobMetadata() {
    ObjectMetadata tempBlobMetaData = new ObjectMetadata();
    tempBlobMetaData.addUserMetadata(TEMPORARY_BLOB_HEADER, "true");
    return tempBlobMetaData;
  }

  private S3Object mockS3Object(String content) {
    S3Object s3Object = mock(S3Object.class);
    S3ObjectInputStream inputStream = new S3ObjectInputStream(new ByteArrayInputStream(content.getBytes()), null);
    when(s3Object.getObjectContent()).thenReturn(inputStream);
    return s3Object;
  }

  private void mockPropertiesException() {
    AmazonS3Exception exception = new AmazonS3Exception("Missing");
    exception.setStatusCode(404);
    when(s3.getObject(anyString(), endsWith(".properties"))).thenThrow(exception);
  }
}
