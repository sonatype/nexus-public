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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.DateBasedLocationStrategy;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.VolumeChapterLocationStrategy;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreMigrationStateProvider;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.api.HeavyBlobRef;
import org.sonatype.nexus.blobstore.api.softdeleted.SoftDeletedBlobIndex;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaUsageChecker;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.S3Blob;
import org.sonatype.nexus.blobstore.s3.internal.datastore.DatastoreS3BlobStoreMetricsService;
import org.sonatype.nexus.common.log.DryRunPrefix;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.EXTERNAL_ETAG_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.EXTERNAL_LAST_MODIFIED_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;
import static org.sonatype.nexus.blobstore.s3.ResponseInputStreamTestUtil.getResponseInputStream;

public class S3BlobStoreTest
    extends TestSupport
{
  @Mock
  private SoftDeletedBlobIndex deletedBlobIndex;

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
  private EncryptingS3Client s3;

  private S3BlobStore blobStore;

  private MockBlobStoreConfiguration config;

  private String attributesContents;

  @Before
  public void setUp() {
    blobStore = createBlobStore();

    config = new MockBlobStoreConfiguration();
    attributesContents = """
            #Thu Jun 01 23:10:55 UTC 2017
            type=s3/1
            @BlobStore.created-by=admin
            size=11
            @Bucket.repo-name=test
            creationTime=1496358655289
            @BlobStore.content-type=text/plain
            @BlobStore.blob-name=test
            sha1=eb4c2a5a1c04ca2d504c5e57e1f88cef08c75707
        """;

    when(amazonS3Factory.create(any())).thenReturn(s3);
    config
        .setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));

    setupCommonS3Mocks();
  }

  private void setupCommonS3Mocks() {
    // Mock metadata operations - by default metadata doesn't exist (only for metadata.properties)
    when(s3.getObjectMetadata(anyString(), eq("myPrefix/metadata.properties")))
        .thenThrow(NoSuchKeyException.builder().build());

    // Mock getObject operations to return proper ResponseInputStream by default
    // Individual tests can override this behavior as needed
    when(s3.getObject(anyString(), anyString()))
        .thenAnswer(invocation -> getResponseInputStream(attributesContents));
  }

  @Test
  public void testGetBlobIdStreamWorksWithPrefix() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    blobStore.init(cfg);
    blobStore.start();

    when(s3.getObjectMetadata(anyString(), anyString())).thenReturn(HeadObjectResponse.builder().build());

    S3Object object1 = S3Object.builder()
        .key("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties")
        .build();
    S3Object object2 = S3Object.builder()
        .key("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes")
        .build();
    when(s3.listObjectsWithPrefix(anyString())).thenReturn(Stream.of(object1, object2));

    List<BlobId> blobIdStream = blobStore.getBlobIdStream().toList();
    assertThat(blobIdStream.size(), is(1));
  }

  @Test
  public void testGetBlobIdUpdatedSinceStream() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    blobStore.init(cfg);
    blobStore.start();

    when(s3.getObjectMetadata(anyString(), anyString())).thenReturn(HeadObjectResponse.builder().build());

    // derived from attributesContents
    OffsetDateTime before =
        LocalDateTime.now().withYear(2017).withMonth(6).withDayOfMonth(1).withHour(0).atOffset(ZoneOffset.UTC);

    S3Object dsStore = S3Object.builder().key("myPrefix/content/vol-01/.DS_Store").build();
    S3Object object1 = S3Object.builder()
        .key("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties")
        .build();
    S3Object object2 = S3Object.builder()
        .key("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.bytes")
        .build();
    S3Object object3 = S3Object.builder()
        .key("myPrefix/content/vol-01/chap-01/12345678-1234-1234-1234-123456789abc.properties")
        .build();
    when(s3.listObjectsWithPrefix(eq("myPrefix/content/vol-")))
        .thenReturn(Stream.of(dsStore, object1, object2, object3));

    List<BlobId> blobIdStream =
        blobStore.getBlobIdUpdatedSinceStream("vol-", before, before.plusDays(1)).toList();
    assertThat(blobIdStream.size(), is(3));
  }

  @Test
  public void testGetBlobIdUpdatedSinceStream_dateBased() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "myPrefix")))));
    blobStore.init(cfg);
    blobStore.start();

    when(s3.getObjectMetadata(anyString(), anyString())).thenReturn(HeadObjectResponse.builder().build());

    // derived from attributesContents
    OffsetDateTime before =
        LocalDateTime.now().withYear(2017).withMonth(6).withDayOfMonth(1).withHour(0).atOffset(ZoneOffset.UTC);

    DateBasedLocationStrategy strategy = new DateBasedLocationStrategy();

    S3Object dsStore = S3Object.builder()
        .key("myPrefix/content/" + strategy.location(new BlobId(".DS_Store", before.plusHours(1))))
        .build();
    S3Object object1 = S3Object.builder()
        .key("myPrefix/content/"
            + strategy.location(new BlobId("12345678-1234-1234-1234-123456789abc", before.plusHours(1)))
            + ".properties")
        .build();
    S3Object object2 = S3Object.builder()
        .key("myPrefix/content/"
            + strategy.location(new BlobId("12345678-1234-1234-1234-123456789abc", before.plusHours(1)))
            + ".bytes")
        .build();
    S3Object object3 = S3Object.builder()
        .key("myPrefix/content/"
            + strategy.location(new BlobId("12345678-1234-1234-1234-123456789abc", before.plusDays(3))) + ".properties")
        .build();
    when(s3.listObjectsWithPrefix(eq("myPrefix/content/2016")))
        .thenReturn(Stream.of(dsStore, object1, object2, object3));

    List<BlobId> blobIdStream =
        blobStore.getBlobIdUpdatedSinceStream("2016", before, before.plusDays(1)).toList();
    assertThat(blobIdStream.size(), is(2));
  }

  @Test
  public void testGetBlobWithBucketPrefix() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket", "prefix", "prefix")))));

    BlobId blobId = new BlobId("test");
    ResponseInputStream<GetObjectResponse> attributesS3Object = getResponseInputStream(attributesContents);
    ResponseInputStream<GetObjectResponse> contentS3Object = getResponseInputStream("hello world");

    doNothing().when(bucketManager).prepareStorageLocation(cfg);
    when(s3.doesObjectExist("mybucket", "prefix/metadata.properties")).thenReturn(false);
    when(s3.getObject("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);
    when(s3.getObject("mybucket", "prefix/" + bytesLocation(blobId))).thenReturn(contentS3Object);

    blobStore.init(cfg);
    blobStore.start();
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
    blobStore.start();
    when(s3.doesObjectExist("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(true);
    ResponseInputStream<GetObjectResponse> attributesS3Object = getResponseInputStream(attributesContents);
    when(s3.getObject("mybucket", "prefix/" + propertiesLocation(blobId))).thenReturn(attributesS3Object);
    boolean deleted = blobStore.delete(blobId, "successful test");

    verify(deletedBlobIndex).createRecord(blobId);
    assertThat(deleted, is(true));
  }

  @Test
  public void testSoftDeleteReturnsFalseWhenBlobDoesNotExist() throws Exception {
    blobStore.init(config);
    blobStore.start();

    // Mock that blob doesn't exist - both properties and bytes files should not exist
    when(s3.getObject(anyString(), contains("soft-delete-fail")))
        .thenThrow(NoSuchKeyException.builder().build());

    boolean deleted = blobStore.delete(new BlobId("soft-delete-fail"), "test");
    assertThat(deleted, is(false));
    verify(s3, never()).setObjectTagging(any());
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
    blobStore.start();

    boolean restored = blobStore.undelete(usageChecker, new BlobId("restore-succeed"), blobAttributes, true);
    assertThat(restored, is(true));
    verify(s3, never()).setObjectTagging(any());

    when(blobAttributes.getMetrics()).thenReturn(mock(BlobMetrics.class));
    BlobId blobId = new BlobId("restore-succeed");
    restored = blobStore.undelete(usageChecker, blobId, blobAttributes, false);
    assertThat(restored, is(true));
    verify(blobAttributes).setDeleted(false);
    verify(blobAttributes).setDeletedReason(null);

    verify(deletedBlobIndex).deleteRecord(blobId);
  }

  @Test
  public void testStartWillAcceptMetadataPropertiesOriginallyCreatedWithFileBlobstore() throws Exception {
    when(s3.doesObjectExist("mybucket", "myPrefix/metadata.properties")).thenReturn(true);
    ResponseInputStream<GetObjectResponse> s3Object = getResponseInputStream("type=file/1");
    when(s3.getObject("mybucket", "myPrefix/metadata.properties")).thenReturn(s3Object);
    blobStore.init(config);
    blobStore.start();
    verify(amazonS3Factory).create(any());
  }

  @Test
  public void testStartRejectsMetadataPropertiesContainingSomethingOtherThanFileOrS3Type() {
    // Mock metadata file exists and has invalid type
    when(s3.doesObjectExist(anyString(), contains("metadata.properties"))).thenReturn(true);
    ResponseInputStream<GetObjectResponse> s3Object = getResponseInputStream("type=other/12");
    when(s3.getObject(anyString(), anyString())).thenReturn(s3Object);
    blobStore.init(config);
    assertThrows(IllegalStateException.class, () -> blobStore.start());
  }

  @Test
  public void testRemoveBucketErrorThrowsException() throws Exception {
    when(s3.listObjectsV2("mybucket", "myPrefix/content/")).thenReturn(ListObjectsV2Response.builder().build());
    blobStore.init(config);
    blobStore.start();
    SdkException s3Exception = S3Exception.builder()
        .awsErrorDetails(AwsErrorDetails.builder()
            .errorCode("UnknownError")
            .errorMessage("error")
            .build())
        .build();
    doThrow(s3Exception).when(bucketManager).deleteStorageLocation(config);
    blobStore.stop();
    assertThrows(BlobStoreException.class, () -> blobStore.remove());
    verify(storeMetrics).remove();
    verify(s3).deleteObject("mybucket", "myPrefix/metadata.properties");
  }

  @Test
  public void testRemoveNonEmptyBucketGeneratesWarningOnly() throws Exception {
    when(s3.listObjectsV2("mybucket", "myPrefix/content/")).thenReturn(ListObjectsV2Response.builder().build());
    blobStore.init(config);
    blobStore.start();

    AwsServiceException s3Exception = S3Exception.builder()
        .awsErrorDetails(AwsErrorDetails.builder()
            .errorCode("BucketNotEmpty")
            .errorMessage("error")
            .build())
        .build();

    doThrow(s3Exception).when(bucketManager).deleteStorageLocation(any());
    blobStore.stop();
    blobStore.remove();
    verify(storeMetrics).remove();
    verify(s3).deleteObject("mybucket", "myPrefix/metadata.properties");
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

    S3Object summary1 = S3Object.builder()
        .key(expectedPropertiesPath)
        .build();
    S3Object summary2 = S3Object.builder()
        .key(expectedBytesPath)
        .build();

    when(s3.listObjectsWithPrefix(anyString())).thenReturn(Stream.of(summary1, summary2));

    blobStore.init(config);
    blobStore.start();

    BlobId blobId = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), Map.of("BlobStore.direct-path",
        "true", "BlobStore.blob-name", "foo/bar/myblob", "BlobStore.created-by", "test")).getId();

    verify(s3).putObject(
        eq(PutObjectRequest.builder().key(expectedPropertiesPath).bucket("mybucket").build()),
        any(RequestBody.class));
    verify(uploader).upload(any(), eq("mybucket"), eq(expectedBytesPath), any());

    List<BlobId> blobIdStream = blobStore.getDirectPathBlobIdStream("foo/bar").toList();
    assertThat(blobIdStream, is(List.of(blobId)));
  }

  @Test
  public void testS3BlobStoreIsWritableWhenClientCanVerifyBucketExists() throws Exception {
    when(s3.doesBucketExist("mybucket")).thenReturn(true);
    blobStore.init(config);
    blobStore.start();
    assertThat(blobStore.isStorageAvailable(), is(true));

    when(s3.doesBucketExist("mybucket")).thenReturn(false);
    assertThat(blobStore.isStorageAvailable(), is(false));

    when(s3.doesBucketExist("mybucket")).thenThrow(SdkClientException.builder().message("Fake error").build());
    assertThat(blobStore.isStorageAvailable(), is(false));
  }

  @Test
  public void testConcurrentAttemptsToRefreshBlobShouldNeverReturnNull() throws Exception {
    MockBlobStoreConfiguration cfg = new MockBlobStoreConfiguration();
    cfg.setAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("bucket", "mybucket")))));
    BlobId blobId = new BlobId("test");
    when(s3.doesObjectExist("mybucket", propertiesLocation(blobId))).thenReturn(true);
    ResponseInputStream<GetObjectResponse> attributesS3Object = getResponseInputStream(attributesContents);
    ResponseInputStream<GetObjectResponse> contentS3Object = getResponseInputStream("hello world");
    when(s3.getObject("mybucket", propertiesLocation(blobId))).thenReturn(attributesS3Object);
    when(s3.getObject("mybucket", bytesLocation(blobId))).thenReturn(contentS3Object);

    blobStore.init(cfg);
    blobStore.start();

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
    blobStore.start();

    Map<String, String> headers = new HashMap<>(Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1",
        BLOB_NAME_HEADER, "temp", TEMPORARY_BLOB_HEADER, ""));
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob.getId().asUniqueString().startsWith("tmp$"), is(false));
    assertThat(blob.getHeaders(), is(headers));

    headers.remove(TEMPORARY_BLOB_HEADER);
    headers.putAll(
        Map.of(BLOB_NAME_HEADER, "file.txt", CONTENT_TYPE_HEADER, "text/plain", REPO_NAME_HEADER, "a repository"));
    blob = blobStore.makeBlobPermanent(blob, headers);

    ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<RequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3, times(3)).putObject(
        putObjectRequestArgumentCaptor.capture(),
        requestBodyArgumentCaptor.capture());

    List<PutObjectRequest> putObjectRequests = putObjectRequestArgumentCaptor.getAllValues();
    assertThat(putObjectRequests.get(1).metadata().get(TEMPORARY_BLOB_HEADER), is("true"));
    assertThat(putObjectRequests.get(2).metadata().containsKey(TEMPORARY_BLOB_HEADER), is(false));

    assertThat(blob.getHeaders(), is(headers));
  }

  @Test
  public void testMakeBlobPermanentThrowsExceptionIfTempBlobHeaderIsPassedIn() throws Exception {
    blobStore.init(config);
    blobStore.start();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "temp",
            TEMPORARY_BLOB_HEADER, "");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThrows(IllegalArgumentException.class, () -> blobStore.makeBlobPermanent(blob, headers)); // NOSONAR
  }

  @Test
  public void makeBlobPermanentAttachExternalHeadersIfPresent() throws Exception {
    BlobId blobId = mock(BlobId.class);
    when(blobId.asUniqueString()).thenReturn("test");

    blobStore.init(config);
    blobStore.start();

    S3BlobStore spy = spy(blobStore);

    Map<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE_HEADER, "application/json");
    headers.put(BLOB_NAME_HEADER, "foo.json");
    headers.put(REPO_NAME_HEADER, "test-repo");

    Blob blob = mock(Blob.class);
    when(blob.getId()).thenReturn(blobId);
    Instant lastModified = Instant.parse("2025-08-18T19:13:17.010Z");
    HeadObjectResponse metadata = HeadObjectResponse.builder()
        .eTag("test-etag")
        .lastModified(lastModified)
        .build();

    doReturn(true).when(spy).isOwner(any());
    doReturn(blob).when(spy).get(blobId);
    doReturn(blob).when(spy).get(blobId, false);
    doReturn(blob).when(spy).writeBlobProperties(blobId, headers);
    when(blob.getHeaders()).thenReturn(Map.of(TEMPORARY_BLOB_HEADER, "true"));
    when(s3.getObjectMetadata(any(), any())).thenReturn(metadata);

    Blob result = spy.makeBlobPermanent(blob, headers);

    verify(spy).getExternalMetadata(blobId);
    assertThat(result, is(blob));
    assertThat(headers.size(), is(5));
    assertThat(headers, hasKey(EXTERNAL_ETAG_HEADER));
    assertThat(headers, hasKey(EXTERNAL_LAST_MODIFIED_HEADER));
    assertThat(headers.get(EXTERNAL_ETAG_HEADER), is("test-etag"));
    assertThat(headers.get(EXTERNAL_LAST_MODIFIED_HEADER), is(lastModified.toString()));
  }

  @Test
  public void makeBlobPermanentDoesNotFailIfNoMetadataAvailable() throws Exception {
    BlobId blobId = mock(BlobId.class);
    when(blobId.asUniqueString()).thenReturn("test");

    blobStore.init(config);
    blobStore.start();

    S3BlobStore spy = spy(blobStore);
    Map<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE_HEADER, "application/parquet");
    headers.put(BLOB_NAME_HEADER, "test.parquet");
    headers.put(REPO_NAME_HEADER, "huggingface");

    S3Blob blob = mock(S3Blob.class);
    when(blob.getId()).thenReturn(blobId);

    doReturn(blob).when(spy).get(blobId);
    doReturn(blob).when(spy).get(blobId, false);
    doReturn(blob).when(spy).writeBlobProperties(blobId, headers);
    doReturn(true).when(spy).isOwner(blob);
    when(blob.getHeaders()).thenReturn(Map.of(TEMPORARY_BLOB_HEADER, "true"));
    when(s3.getObjectMetadata(any(), any()))
        .thenThrow(SdkClientException.builder().message("unable to get metadata").build());

    Blob result = spy.makeBlobPermanent(blob, headers);

    verify(spy).getExternalMetadata(blobId);
    assertThat(result, is(blob));
    assertThat(headers.size(), is(3));
    assertThat(headers, not(hasKey(EXTERNAL_ETAG_HEADER)));
    assertThat(headers, not(hasKey(EXTERNAL_LAST_MODIFIED_HEADER)));
  }

  @Test
  public void testIsOwner() throws Exception {
    blobStore.init(config);
    blobStore.start();

    assertThat("Unknown blob should return null", blobStore.isOwner(mock(Blob.class)), is(false));

    S3BlobStore otherStore = createBlobStore();
    otherStore.init(config.copy("some-other-blobstore"));
    otherStore.start();
    Blob blob = otherStore.create(new ByteArrayInputStream(new byte[0]),
        Map.of(BLOB_NAME_HEADER, "foo", CREATED_BY_HEADER, "jsmith"));
    assertThat("Blob owned by different instance should return false", blobStore.isOwner(blob), is(false));

    blob = blobStore.create(new ByteArrayInputStream(new byte[0]),
        Map.of(BLOB_NAME_HEADER, "foo", CREATED_BY_HEADER, "jsmith"));
    assertThat(blobStore.isOwner(blob), is(true));
  }

  @Test
  public void testDeleteIfTempDeletesBlobWhenTempBlobHeaderIsPresent() throws Exception {
    blobStore.init(config);
    blobStore.start();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "temp",
            TEMPORARY_BLOB_HEADER, "");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob, is(notNullValue()));

    DeleteObjectsResponse deleteObjectsResult = mock(DeleteObjectsResponse.class);
    when(deleteObjectsResult.deleted()).thenReturn(List.of(
        DeletedObject.builder().key("key1").build(),
        DeletedObject.builder().key("key2").build()));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResult);

    boolean deleted = blobStore.deleteIfTemp(blob);
    assertThat(deleted, is(true));

    mockPropertiesException();
    Blob retrievedBlob = blobStore.get(blob.getId());
    assertThat(retrievedBlob, is(nullValue()));
    verify(s3).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  public void testDeleteIfTempDoesNotDeleteBlobWhenTempBlobHeaderIsAbsent() throws Exception {
    blobStore.init(config);
    blobStore.start();

    Map<String, String> headers =
        Map.of(CREATED_BY_HEADER, "test", CREATED_BY_IP_HEADER, "127.0.0.1", BLOB_NAME_HEADER, "file.txt",
            CONTENT_TYPE_HEADER, "text/plain", REPO_NAME_HEADER, "a repository");
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers);

    assertThat(blob, is(notNullValue()));

    boolean deleted = blobStore.deleteIfTemp(blob);
    assertThat(deleted, is(true));
    Blob retrievedBlob = blobStore.get(blob.getId());
    assertThat(retrievedBlob, is(notNullValue()));
    verify(s3, never()).deleteObjects(any());
  }

  @Test
  public void testStopShouldStopMetricsService() throws Exception {
    blobStore.init(config);
    blobStore.start();

    blobStore.stop();

    verify(storeMetrics).stop();
    verify(blobStoreQuotaUsageChecker).stop();
  }

  private S3BlobStore createBlobStore() {
    S3BlobStore blobstore = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(), uploader, copier,
        false, storeMetrics, deletedBlobIndex, dryRunPrefix, bucketManager, blobStoreQuotaUsageChecker, true, null);

    ReflectionTestUtils.setField(blobstore, "maxRetries", 1);
    ReflectionTestUtils.setField(blobstore, "retryDelayMs", 500L);
    return blobstore;
  }

  private static String propertiesLocation(final BlobId blobId) {
    return "content/" + new VolumeChapterLocationStrategy().location(blobId) + ".properties";
  }

  private static String bytesLocation(final BlobId blobId) {
    return "content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";
  }

  private static HeadObjectResponse getTempBlobMetadata() {
    return HeadObjectResponse.builder()
        .metadata(Map.of(TEMPORARY_BLOB_HEADER, "true"))
        .build();
  }

  private void mockPropertiesException() {
    when(s3.getObject(anyString(), contains("properties")))
        .thenThrow(NoSuchKeyException.builder().build());
  }

  @Test
  public void testCreateWithAssignedBlobIdOverridesTimestamp() throws Exception {
    blobStore.init(config);
    blobStore.start();

    // Create a BlobId with an old timestamp (2 hours ago)
    OffsetDateTime oldTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2);
    BlobId assignedBlobId = new BlobId("12345678-1234-1234-1234-123456789abc", oldTimestamp);

    Map<String, String> headers = Map.of(
        CREATED_BY_HEADER, "test",
        CREATED_BY_IP_HEADER, "127.0.0.1",
        BLOB_NAME_HEADER, "test-file.txt",
        CONTENT_TYPE_HEADER, "text/plain",
        REPO_NAME_HEADER, "test-repo");

    // Create blob with assigned BlobId
    Blob blob = blobStore.create(new ByteArrayInputStream("hello world".getBytes()), headers, assignedBlobId);

    // Assert that the blob was created
    assertThat(blob, notNullValue());

    // Assert that the UUID string is preserved
    assertThat(blob.getId().asUniqueString(), is("12345678-1234-1234-1234-123456789abc"));

    // Assert that the timestamp was overridden to current time (not the old timestamp)
    assertThat(blob.getId().getBlobCreatedRef(), notNullValue());
    assertThat(blob.getId().getBlobCreatedRef(), is(not(oldTimestamp)));

    // Assert that the new timestamp is recent (within last minute)
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(blob.getId().getBlobCreatedRef().isAfter(now.minusMinutes(1)), is(true));
    assertThat(blob.getId().getBlobCreatedRef().isBefore(now.plusSeconds(5)), is(true));
  }

  @Test
  public void testGet_heavyBlobRef() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    Blob originalCachedBlob = blobStore.get(blobId);

    BlobMetrics metrics = mock();
    Blob dbDerivedBlob = blobStore.get(new HeavyBlobRef(new BlobRef("blobstore", blobId.asUniqueString()), metrics));

    assertThat(originalCachedBlob, not(is(dbDerivedBlob)));
    // The original blob's headers should not be lost
    assertThat(originalCachedBlob.getHeaders().keySet(), not(empty()));

    assertThat(dbDerivedBlob.getMetrics(), is(metrics));
  }

  @Test
  public void testBlobExistsInStorage_blobExists() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock successful HEAD request (blob exists)
    HeadObjectResponse headResponse = mock(HeadObjectResponse.class);
    when(s3.getObjectMetadata("mybucket", path)).thenReturn(headResponse);

    // Test
    boolean exists = blobStore.blobExistsInStorage(blobId);

    assertThat(exists, is(true));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testBlobExistsInStorage_blobDoesNotExist() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock NoSuchKeyException (blob doesn't exist - 404)
    NoSuchKeyException noSuchKeyException = (NoSuchKeyException) NoSuchKeyException.builder()
        .message("The specified key does not exist")
        .build();
    when(s3.getObjectMetadata("mybucket", path)).thenThrow(noSuchKeyException);

    // Test
    boolean exists = blobStore.blobExistsInStorage(blobId);

    assertThat(exists, is(false));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testBlobExistsInStorage_s3Exception() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock S3 exception (not 404 - could be permission error, network timeout, etc.)
    S3Exception s3Exception = (S3Exception) S3Exception.builder()
        .message("Access Denied")
        .statusCode(403)
        .build();
    when(s3.getObjectMetadata("mybucket", path)).thenThrow(s3Exception);

    // Test - should return true to avoid false negatives
    boolean exists = blobStore.blobExistsInStorage(blobId);

    assertThat(exists, is(true));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testBlobExistsInStorage_genericException() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock generic exception (network error, etc.)
    when(s3.getObjectMetadata("mybucket", path)).thenThrow(new RuntimeException("Network error"));

    // Test - should return true to avoid false negatives
    boolean exists = blobStore.blobExistsInStorage(blobId);

    assertThat(exists, is(true));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testGet_heavyBlobRefReturnsNullWhenBlobDoesNotExistInStorage() throws Exception {
    // Create blob store with migration state provider that indicates migration is active
    BlobStoreMigrationStateProvider migrationStateProvider = mock(BlobStoreMigrationStateProvider.class);
    when(migrationStateProvider.isMigrationActive()).thenReturn(true);

    S3BlobStore blobStoreWithMigration = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(),
        uploader, copier, false, storeMetrics, deletedBlobIndex, dryRunPrefix, bucketManager,
        blobStoreQuotaUsageChecker, true, migrationStateProvider);
    ReflectionTestUtils.setField(blobStoreWithMigration, "maxRetries", 1);
    ReflectionTestUtils.setField(blobStoreWithMigration, "retryDelayMs", 500L);

    blobStoreWithMigration.init(config);
    blobStoreWithMigration.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock NoSuchKeyException - blob doesn't exist in S3 storage
    NoSuchKeyException noSuchKeyException = (NoSuchKeyException) NoSuchKeyException.builder()
        .message("The specified key does not exist")
        .build();
    when(s3.getObjectMetadata("mybucket", path)).thenThrow(noSuchKeyException);

    // Also mock getObject to throw exception for both properties and bytes
    when(s3.getObject("mybucket", path)).thenThrow(noSuchKeyException);
    when(s3.getObject(eq("mybucket"), contains(".properties"))).thenThrow(noSuchKeyException);

    BlobMetrics metrics = mock();
    HeavyBlobRef heavyBlobRef = new HeavyBlobRef(new BlobRef("blobstore", blobId.asUniqueString()), metrics);

    // Test - should return null to trigger fallback mechanism (NEXUS-50554)
    Blob result = blobStoreWithMigration.get(heavyBlobRef);

    assertThat(result, is(nullValue()));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testGet_heavyBlobRefReturnsNonNullWhenBlobExistsInStorage() throws Exception {
    // Create blob store with migration state provider that indicates migration is active
    BlobStoreMigrationStateProvider migrationStateProvider = mock(BlobStoreMigrationStateProvider.class);
    when(migrationStateProvider.isMigrationActive()).thenReturn(true);

    S3BlobStore blobStoreWithMigration = new S3BlobStore(amazonS3Factory, new DefaultBlobIdLocationResolver(),
        uploader, copier, false, storeMetrics, deletedBlobIndex, dryRunPrefix, bucketManager,
        blobStoreQuotaUsageChecker, true, migrationStateProvider);
    ReflectionTestUtils.setField(blobStoreWithMigration, "maxRetries", 1);
    ReflectionTestUtils.setField(blobStoreWithMigration, "retryDelayMs", 500L);

    blobStoreWithMigration.init(config);
    blobStoreWithMigration.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "myPrefix/content/" + new VolumeChapterLocationStrategy().location(blobId) + ".bytes";

    // Mock successful HEAD request - blob exists
    HeadObjectResponse headResponse = mock(HeadObjectResponse.class);
    when(s3.getObjectMetadata("mybucket", path)).thenReturn(headResponse);

    BlobMetrics metrics = mock();
    HeavyBlobRef heavyBlobRef = new HeavyBlobRef(new BlobRef("blobstore", blobId.asUniqueString()), metrics);

    // Test - should return non-null blob
    Blob result = blobStoreWithMigration.get(heavyBlobRef);

    assertThat(result, is(notNullValue()));
    assertThat(result.getMetrics(), is(metrics));
    verify(s3).getObjectMetadata("mybucket", path);
  }

  @Test
  public void testGet_regularBlobRefNotAffectedByExistenceCheck() throws Exception {
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId(UUID.randomUUID().toString());
    String path = "content/" + blobId.asUniqueString() + ".bytes";

    // Mock NoSuchKeyException - blob doesn't exist
    NoSuchKeyException noSuchKeyException = NoSuchKeyException.builder()
        .message("The specified key does not exist")
        .build();
    when(s3.getObjectMetadata("mybucket", path)).thenThrow(noSuchKeyException);

    // Use regular BlobRef (not HeavyBlobRef) - should use standard get(BlobId) path
    BlobRef regularBlobRef = new BlobRef("blobstore", blobId.asUniqueString());
    Blob result = blobStore.get(regularBlobRef);

    // Regular BlobRef should use get(BlobId) which doesn't check existence
    // getObjectMetadata should NOT be called for regular BlobRef
    verify(s3, never()).getObjectMetadata(anyString(), anyString());
  }
}
