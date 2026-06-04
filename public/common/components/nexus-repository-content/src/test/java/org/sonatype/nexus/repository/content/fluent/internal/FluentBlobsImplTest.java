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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.AttachableBlob;
import org.sonatype.nexus.repository.view.payloads.DetachedBlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPayload;
import org.sonatype.nexus.security.ClientInfo;

import com.google.common.hash.HashCode;
import jakarta.inject.Provider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FluentBlobsImplTest
{
  private static final String REPO_NAME = "test-repo";

  private static final String USER_ID = "testuser";

  private static final String REMOTE_IP = "127.0.0.1";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ContentFacetSupport facet;

  @Mock
  private Provider<BlobStore> blobStoreProvider;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Repository repository;

  @Mock
  private BlobStoreMetrics blobStoreMetrics;

  private FluentBlobsImpl underTest;

  @Before
  public void setUp() {
    when(blobStoreProvider.get()).thenReturn(blobStore);
    when(repository.getName()).thenReturn(REPO_NAME);
    when(facet.repository()).thenReturn(repository);

    ClientInfo clientInfo = ClientInfo.builder()
        .userId(USER_ID)
        .remoteIP(REMOTE_IP)
        .build();
    when(facet.clientInfo()).thenReturn(Optional.of(clientInfo));

    underTest = new FluentBlobsImpl(facet, blobStoreProvider);
  }

  @Test
  public void testBlobReturnsPresent() {
    BlobRef blobRef = new BlobRef("default", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    Blob blob = mock(Blob.class);
    when(blobStore.get(any(BlobId.class))).thenReturn(blob);

    Optional<Blob> result = underTest.blob(blobRef);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(blob));
  }

  @Test
  public void testBlobReturnsEmpty() {
    BlobRef blobRef = new BlobRef("default", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    when(blobStore.get(any(BlobId.class))).thenReturn(null);

    Optional<Blob> result = underTest.blob(blobRef);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testGetMetrics() {
    when(blobStore.getMetrics()).thenReturn(blobStoreMetrics);

    BlobStoreMetrics result = underTest.getMetrics();

    assertThat(result, is(notNullValue()));
    assertThat(result, is(blobStoreMetrics));
    verify(blobStore).getMetrics();
  }

  @Test
  public void testIngestInputStreamWithContentType() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(inputStream, "text/plain", hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
    verify(blobStore).create(any(InputStream.class), any(Map.class));
  }

  @Test
  public void testIngestInputStreamWithNullContentType() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(inputStream, null, hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test
  public void testIngestInputStreamWithHeaders() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    Map<String, String> headers = Map.of("custom-header", "custom-value");
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(inputStream, "text/plain", headers, hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test
  public void testIngestPayloadOpensInputStream() throws Exception {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    Payload payload = mock(Payload.class);
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(payload.getContentType()).thenReturn("application/json");

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test
  public void testIngestPayloadWithNullContentType() throws Exception {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    Payload payload = mock(Payload.class);
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(payload.getContentType()).thenReturn(null);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testIngestTempBlobPayloadReturnsTempBlob() {
    TempBlob tempBlob = mock(TempBlob.class);
    TempBlobPayload payload = mock(TempBlobPayload.class);
    when(payload.getTempBlob()).thenReturn(tempBlob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, Map.of(), hashing);

    assertThat(result, is(tempBlob));
  }

  @Test
  public void testIngestDetachedBlobPayloadReturnsAttachableBlob() throws Exception {
    Blob detachedBlob = mock(Blob.class);
    when(detachedBlob.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    Map<String, String> detachedHeaders = Map.of(CONTENT_TYPE_HEADER, "text/plain");
    when(detachedBlob.getHeaders()).thenReturn(detachedHeaders);

    DetachedBlobPayload payload = new DetachedBlobPayload(detachedBlob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result, is(instanceOf(AttachableBlob.class)));
  }

  @Test
  public void testIngestContentPayloadUnwrapsInnerPayload() throws Exception {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    Payload innerPayload = mock(Payload.class);
    when(innerPayload.openInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(innerPayload.getContentType()).thenReturn("text/xml");

    Content content = new Content(innerPayload);
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(content, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test
  public void testIngestSrcBlobSameStore() {
    Blob srcBlob = mock(Blob.class);
    BlobId srcBlobId = new BlobId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    when(srcBlob.getId()).thenReturn(srcBlobId);
    Map<String, String> srcHeaders = Map.of(CONTENT_TYPE_HEADER, "application/xml");
    when(srcBlob.getHeaders()).thenReturn(srcHeaders);

    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);
    when(destConfig.getName()).thenReturn("default");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(destConfig);

    BlobStore srcStore = mock(BlobStore.class);
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    when(srcConfig.getName()).thenReturn("default");
    when(srcStore.getBlobStoreConfiguration()).thenReturn(srcConfig);

    Map<HashAlgorithm, HashCode> hashes =
        Map.of(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    TempBlob result = underTest.ingest(srcBlob, srcStore, hashes);

    assertThat(result, is(notNullValue()));
    assertThat(result, is(instanceOf(AttachableBlob.class)));
    AttachableBlob attachableBlob = (AttachableBlob) result;
    assertThat(attachableBlob.getBlob(), is(srcBlob));
    // verify copy was NOT called since we re-attach instead
    verify(blobStore, never()).copy(any(BlobId.class), any(Map.class));
  }

  @Test
  public void testIngestSrcBlobDifferentStore() throws Exception {
    Blob srcBlob = mock(Blob.class);
    BlobId srcBlobId = new BlobId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    when(srcBlob.getId()).thenReturn(srcBlobId);
    Map<String, String> srcHeaders = Map.of(CONTENT_TYPE_HEADER, "application/xml");
    when(srcBlob.getHeaders()).thenReturn(srcHeaders);
    when(srcBlob.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);
    when(destConfig.getName()).thenReturn("dest-store");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(destConfig);

    BlobStore srcStore = mock(BlobStore.class);
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    when(srcConfig.getName()).thenReturn("src-store");
    when(srcStore.getBlobStoreConfiguration()).thenReturn(srcConfig);

    Blob createdBlob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(createdBlob);

    Map<HashAlgorithm, HashCode> hashes =
        Map.of(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    TempBlob result = underTest.ingest(srcBlob, srcStore, hashes);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(createdBlob));
    verify(blobStore).create(any(InputStream.class), any(Map.class));
  }

  @Test
  public void testTempHeadersWithClientInfo() throws Exception {
    Method tempHeadersMethod = FluentBlobsImpl.class.getDeclaredMethod(
        "tempHeaders", Map.class, String.class);
    tempHeadersMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) tempHeadersMethod.invoke(
        underTest, Map.of(), "text/plain");

    assertThat(headers, is(notNullValue()));
    assertThat(headers.get(REPO_NAME_HEADER), is(REPO_NAME));
    assertThat(headers.get(CREATED_BY_HEADER), is(USER_ID));
    assertThat(headers.get(CREATED_BY_IP_HEADER), is(REMOTE_IP));
    assertThat(headers.get(CONTENT_TYPE_HEADER), is("text/plain"));
    assertThat(headers.get(BLOB_NAME_HEADER), is("temp"));
    assertThat(headers.get(TEMPORARY_BLOB_HEADER), is(""));
  }

  @Test
  public void testTempHeadersWithoutClientInfo() throws Exception {
    when(facet.clientInfo()).thenReturn(Optional.empty());

    Method tempHeadersMethod = FluentBlobsImpl.class.getDeclaredMethod(
        "tempHeaders", Map.class, String.class);
    tempHeadersMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) tempHeadersMethod.invoke(
        underTest, Map.of(), "text/plain");

    assertThat(headers, is(notNullValue()));
    assertThat(headers.get(CREATED_BY_HEADER), is("system"));
    assertThat(headers.get(CREATED_BY_IP_HEADER), is("system"));
  }

  @Test
  public void testTempHeadersWithNullContentTypeDefaultsToOctetStream() throws Exception {
    Method tempHeadersMethod = FluentBlobsImpl.class.getDeclaredMethod(
        "tempHeaders", Map.class, String.class);
    tempHeadersMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) tempHeadersMethod.invoke(
        underTest, Map.of(), (String) null);

    assertThat(headers, is(notNullValue()));
    assertThat(headers.get(CONTENT_TYPE_HEADER), is("application/octet-stream"));
  }

  @Test
  public void testTempHeadersExistingHeadersNotOverridden() throws Exception {
    Method tempHeadersMethod = FluentBlobsImpl.class.getDeclaredMethod(
        "tempHeaders", Map.class, String.class);
    tempHeadersMethod.setAccessible(true);

    Map<String, String> existingHeaders = Map.of(
        BLOB_NAME_HEADER, "custom-name",
        CREATED_BY_HEADER, "custom-user");

    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) tempHeadersMethod.invoke(
        underTest, existingHeaders, "text/plain");

    assertThat(headers, is(notNullValue()));
    // existing headers should be preserved
    assertThat(headers.get(BLOB_NAME_HEADER), is("custom-name"));
    assertThat(headers.get(CREATED_BY_HEADER), is("custom-user"));
    // repo name is always overridden
    assertThat(headers.get(REPO_NAME_HEADER), is(REPO_NAME));
  }

  @Test
  public void testCleanupContentTypeReturnsNullForNull() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, new Object[]{null});

    assertThat(result == null, is(true));
  }

  @Test
  public void testCleanupContentTypeReturnsContentTypeWithoutSemicolon() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, "application/json");

    assertThat(result, is("application/json"));
  }

  @Test
  public void testCleanupContentTypeStripsAfterSemicolon() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, "text/html; charset=UTF-8");

    assertThat(result, is("text/html"));
  }

  @Test
  public void testCleanupContentTypeStripsMultipleSemicolons() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, "text/html; charset=UTF-8; boundary=something");

    assertThat(result, is("text/html"));
  }

  @Test
  public void testIngestPayloadContentTypeCleanup() throws Exception {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    Payload payload = mock(Payload.class);
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(payload.getContentType()).thenReturn("text/html; charset=UTF-8");

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullFacet() {
    new FluentBlobsImpl(null, blobStoreProvider);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullBlobStoreProvider() {
    new FluentBlobsImpl(facet, null);
  }

  @Test
  public void testIngestPathWithHeadersAndSha1() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(java.nio.file.Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    java.nio.file.Path sourceFile = mock(java.nio.file.Path.class);
    Map<String, String> headers = Map.of(BLOB_NAME_HEADER, "test-blob");
    HashCode sha1 = HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709");

    Blob result = underTest.ingest(sourceFile, headers, sha1, 100L);

    assertThat(result, is(notNullValue()));
    assertThat(result, is(blob));
    verify(blobStore).create(any(java.nio.file.Path.class), any(Map.class), anyLong(), any(HashCode.class));
  }

  @Test
  public void testIngestPathWithHeadersAddsRepoAndCreatedByHeaders() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(java.nio.file.Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    java.nio.file.Path sourceFile = mock(java.nio.file.Path.class);
    Map<String, String> headers = Map.of(BLOB_NAME_HEADER, "test-blob");
    HashCode sha1 = HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709");

    underTest.ingest(sourceFile, headers, sha1, 100L);

    verify(blobStore).create(any(java.nio.file.Path.class), any(Map.class), anyLong(), any(HashCode.class));
  }

  @Test
  public void testIngestPayloadIOExceptionWrapped() throws Exception {
    Payload payload = mock(Payload.class);
    when(payload.openInputStream()).thenThrow(new IOException("test IO error"));
    when(payload.getContentType()).thenReturn("text/plain");

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    try {
      underTest.ingest(payload, Map.of(), hashing);
      assertThat("Should have thrown exception", false, is(true));
    }
    catch (java.io.UncheckedIOException e) {
      assertThat(e.getCause().getMessage(), is("test IO error"));
    }
  }

  @Test
  public void testIngestPathWithHardLinkSuccess() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-hardlink.txt").toPath();
    Files.writeString(tempFile, "hard link content");

    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, "text/plain", hashing, true);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
    verify(blobStore).create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class));
    // stream-based create should not be called when hard link succeeds
    verify(blobStore, never()).create(any(InputStream.class), any(Map.class));
  }

  @Test
  public void testIngestPathWithHardLinkFailureFallsBackToStream() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-fallback.txt").toPath();
    Files.writeString(tempFile, "fallback content");

    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenThrow(new RuntimeException("hard link not supported"));

    Blob streamBlob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(streamBlob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, "application/xml", hashing, false);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(streamBlob));
    // hard link was attempted then stream fallback was used
    verify(blobStore).create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class));
    verify(blobStore).create(any(InputStream.class), any(Map.class));
  }

  @Test(expected = RuntimeException.class)
  public void testIngestPathWithHardLinkFailureRethrowsWhenRequired() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-rethrow.txt").toPath();
    Files.writeString(tempFile, "rethrow content");

    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenThrow(new RuntimeException("hard link not supported"));

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    underTest.ingest(tempFile, "text/plain", hashing, true);
  }

  @Test
  public void testIngestPathWithNullContentTypeUsesOctetStream() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-null-ct.txt").toPath();
    Files.writeString(tempFile, "null content type");

    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, null, hashing, true);

    assertThat(result, is(notNullValue()));
    // verify the headers passed to create contain the default content type
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(Path.class), headersCaptor.capture(), anyLong(), any(HashCode.class));
    assertThat(headersCaptor.getValue().get(CONTENT_TYPE_HEADER), is("application/octet-stream"));
  }

  @Test
  public void testIngestSrcBlobDifferentStoreIOExceptionWrapped() throws Exception {
    Blob srcBlob = mock(Blob.class);
    BlobId srcBlobId = new BlobId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    when(srcBlob.getId()).thenReturn(srcBlobId);
    Map<String, String> srcHeaders = Map.of(CONTENT_TYPE_HEADER, "application/xml");
    when(srcBlob.getHeaders()).thenReturn(srcHeaders);
    // return an InputStream whose close() throws IOException to trigger the catch block
    InputStream failingStream = new InputStream()
    {
      @Override
      public int read() {
        return -1;
      }

      @Override
      public void close() throws IOException {
        throw new IOException("stream close failure");
      }
    };
    when(srcBlob.getInputStream()).thenReturn(failingStream);

    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);
    when(destConfig.getName()).thenReturn("dest-store");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(destConfig);

    BlobStore srcStore = mock(BlobStore.class);
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    when(srcConfig.getName()).thenReturn("src-store");
    when(srcStore.getBlobStoreConfiguration()).thenReturn(srcConfig);

    Blob createdBlob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(createdBlob);

    Map<HashAlgorithm, HashCode> hashes =
        Map.of(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    try {
      underTest.ingest(srcBlob, srcStore, hashes);
      assertThat("Should have thrown exception", false, is(true));
    }
    catch (UncheckedIOException e) {
      assertThat(e.getCause().getMessage(), is("stream close failure"));
    }
  }

  @Test
  public void testIngestPathDirectWithHeadersNoClientInfo() {
    when(facet.clientInfo()).thenReturn(Optional.empty());

    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Path sourceFile = mock(Path.class);
    Map<String, String> headers = Map.of(BLOB_NAME_HEADER, "test-blob");
    HashCode sha1 = HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709");

    Blob result = underTest.ingest(sourceFile, headers, sha1, 100L);

    assertThat(result, is(blob));
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(Path.class), headersCaptor.capture(), eq(100L), eq(sha1));
    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(CREATED_BY_HEADER), is("system"));
    assertThat(capturedHeaders.get(CREATED_BY_IP_HEADER), is("system"));
    assertThat(capturedHeaders.get(REPO_NAME_HEADER), is(REPO_NAME));
  }

  @Test
  public void testIngestPathDirectWithExistingRepoAndCreatedByHeaders() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Path sourceFile = mock(Path.class);
    Map<String, String> headers = new HashMap<>();
    headers.put(BLOB_NAME_HEADER, "test-blob");
    headers.put(REPO_NAME_HEADER, "existing-repo");
    headers.put(CREATED_BY_HEADER, "existing-user");
    headers.put(CREATED_BY_IP_HEADER, "10.0.0.1");
    HashCode sha1 = HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709");

    Blob result = underTest.ingest(sourceFile, headers, sha1, 200L);

    assertThat(result, is(blob));
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(Path.class), headersCaptor.capture(), eq(200L), eq(sha1));
    Map<String, String> capturedHeaders = headersCaptor.getValue();
    // existing headers should NOT be overridden by maybePut
    assertThat(capturedHeaders.get(REPO_NAME_HEADER), is("existing-repo"));
    assertThat(capturedHeaders.get(CREATED_BY_HEADER), is("existing-user"));
    assertThat(capturedHeaders.get(CREATED_BY_IP_HEADER), is("10.0.0.1"));
  }

  @Test
  public void testIngestContentWrappingTempBlobPayloadReturnsTempBlob() {
    TempBlob tempBlob = mock(TempBlob.class);
    TempBlobPayload tempBlobPayload = mock(TempBlobPayload.class);
    when(tempBlobPayload.getTempBlob()).thenReturn(tempBlob);

    Content content = new Content(tempBlobPayload);
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(content, Map.of(), hashing);

    assertThat(result, is(tempBlob));
  }

  @Test
  public void testIngestContentWrappingDetachedBlobPayloadReturnsAttachableBlob() throws Exception {
    Blob detachedBlob = mock(Blob.class);
    when(detachedBlob.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    Map<String, String> detachedHeaders = Map.of(CONTENT_TYPE_HEADER, "text/plain");
    when(detachedBlob.getHeaders()).thenReturn(detachedHeaders);

    DetachedBlobPayload detachedPayload = new DetachedBlobPayload(detachedBlob);
    Content content = new Content(detachedPayload);
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(content, Map.of(), hashing);

    assertThat(result, is(notNullValue()));
    assertThat(result, is(instanceOf(AttachableBlob.class)));
  }

  @Test
  public void testTempHeadersAllDefaultsOverriddenByExistingHeaders() throws Exception {
    Method tempHeadersMethod = FluentBlobsImpl.class.getDeclaredMethod(
        "tempHeaders", Map.class, String.class);
    tempHeadersMethod.setAccessible(true);

    Map<String, String> existingHeaders = Map.of(
        TEMPORARY_BLOB_HEADER, "custom-temp",
        BLOB_NAME_HEADER, "custom-name",
        CREATED_BY_HEADER, "custom-user",
        CREATED_BY_IP_HEADER, "192.168.1.1",
        CONTENT_TYPE_HEADER, "custom/type");

    @SuppressWarnings("unchecked")
    Map<String, String> headers = (Map<String, String>) tempHeadersMethod.invoke(
        underTest, existingHeaders, "text/plain");

    assertThat(headers, is(notNullValue()));
    // all pre-existing headers should be preserved via maybePut
    assertThat(headers.get(TEMPORARY_BLOB_HEADER), is("custom-temp"));
    assertThat(headers.get(BLOB_NAME_HEADER), is("custom-name"));
    assertThat(headers.get(CREATED_BY_HEADER), is("custom-user"));
    assertThat(headers.get(CREATED_BY_IP_HEADER), is("192.168.1.1"));
    assertThat(headers.get(CONTENT_TYPE_HEADER), is("custom/type"));
    // repo name is always set (via putAll then put, not maybePut)
    assertThat(headers.get(REPO_NAME_HEADER), is(REPO_NAME));
  }

  @Test
  public void testIngestPathComputesHashesCorrectly() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-hashes.txt").toPath();
    Files.writeString(tempFile, "hash me");

    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, "text/plain", hashing, true);

    assertThat(result, is(notNullValue()));
    // the SHA1 hash should have been computed and passed to blobStore.create
    ArgumentCaptor<HashCode> hashCaptor = ArgumentCaptor.forClass(HashCode.class);
    verify(blobStore).create(any(Path.class), any(Map.class), anyLong(), hashCaptor.capture());
    assertThat(hashCaptor.getValue(), is(notNullValue()));
  }

  @Test
  public void testIngestPathFallbackStreamAlsoPassesTempHeaders() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-fallback-headers.txt").toPath();
    Files.writeString(tempFile, "fallback content");

    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenThrow(new RuntimeException("hard link not supported"));

    Blob streamBlob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(streamBlob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, "application/json", hashing, false);

    assertThat(result, is(notNullValue()));
    // verify that the stream fallback received proper temp headers
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(InputStream.class), headersCaptor.capture());
    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(REPO_NAME_HEADER), is(REPO_NAME));
    assertThat(capturedHeaders.get(CONTENT_TYPE_HEADER), is("application/json"));
    assertThat(capturedHeaders.get(BLOB_NAME_HEADER), is("temp"));
    assertThat(capturedHeaders.get(TEMPORARY_BLOB_HEADER), is(""));
  }

  @Test
  public void testIngestInputStreamDelegatesToFourArgOverload() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    InputStream inputStream = new ByteArrayInputStream("delegate test".getBytes());
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(inputStream, "text/plain", hashing);

    assertThat(result, is(notNullValue()));
    // verify the headers include defaults from tempHeaders
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(InputStream.class), headersCaptor.capture());
    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(REPO_NAME_HEADER), is(REPO_NAME));
    assertThat(capturedHeaders.get(CREATED_BY_HEADER), is(USER_ID));
    assertThat(capturedHeaders.get(CREATED_BY_IP_HEADER), is(REMOTE_IP));
    assertThat(capturedHeaders.get(CONTENT_TYPE_HEADER), is("text/plain"));
    assertThat(capturedHeaders.get(TEMPORARY_BLOB_HEADER), is(""));
    assertThat(capturedHeaders.get(BLOB_NAME_HEADER), is("temp"));
  }

  @Test
  public void testIngestInputStreamWithCustomHeadersPreservesCustomValues() {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    InputStream inputStream = new ByteArrayInputStream("custom headers test".getBytes());
    Map<String, String> headers = Map.of(
        BLOB_NAME_HEADER, "my-blob",
        CREATED_BY_HEADER, "override-user",
        "X-Custom", "custom-value");
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(inputStream, "text/plain", headers, hashing);

    assertThat(result, is(notNullValue()));
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(InputStream.class), headersCaptor.capture());
    Map<String, String> capturedHeaders = headersCaptor.getValue();
    // custom headers should be preserved, not overridden by defaults
    assertThat(capturedHeaders.get(BLOB_NAME_HEADER), is("my-blob"));
    assertThat(capturedHeaders.get(CREATED_BY_HEADER), is("override-user"));
    assertThat(capturedHeaders.get("X-Custom"), is("custom-value"));
    // repo name is always set
    assertThat(capturedHeaders.get(REPO_NAME_HEADER), is(REPO_NAME));
  }

  @Test
  public void testIngestPayloadWithHeadersPassesHeadersThrough() throws Exception {
    Blob blob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(blob);

    Payload payload = mock(Payload.class);
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(payload.getContentType()).thenReturn("application/json");

    Map<String, String> headers = Map.of("X-Custom-Header", "custom-value");
    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(payload, headers, hashing);

    assertThat(result, is(notNullValue()));
    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(blobStore).create(any(InputStream.class), headersCaptor.capture());
    assertThat(headersCaptor.getValue().get("X-Custom-Header"), is("custom-value"));
  }

  @Test
  public void testCleanupContentTypeEmptyString() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, "");

    assertThat(result, is(""));
  }

  @Test
  public void testCleanupContentTypeSemicolonAtStart() throws Exception {
    Method cleanupMethod = FluentBlobsImpl.class.getDeclaredMethod("cleanupContentType", String.class);
    cleanupMethod.setAccessible(true);

    String result = (String) cleanupMethod.invoke(underTest, "; charset=UTF-8");

    assertThat(result, is(""));
  }

  @Test
  public void testIngestPathWithMultipleHashAlgorithms() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-multi-hash.txt").toPath();
    Files.writeString(tempFile, "multi hash content");

    Blob blob = mock(Blob.class);
    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenReturn(blob);

    Iterable<HashAlgorithm> hashing = java.util.List.of(HashAlgorithm.SHA1, HashAlgorithm.SHA256, HashAlgorithm.MD5);

    TempBlob result = underTest.ingest(tempFile, "text/plain", hashing, true);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(blob));
  }

  @Test
  public void testIngestPathHardLinkFailureWithUncheckedExceptionFallsBack() throws Exception {
    Path tempFile = temporaryFolder.newFile("test-uncheck-fallback.txt").toPath();
    Files.writeString(tempFile, "unchecked fallback content");

    when(blobStore.create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class)))
        .thenThrow(new UnsupportedOperationException("hard links not supported on this filesystem"));

    Blob streamBlob = mock(Blob.class);
    when(blobStore.create(any(InputStream.class), any(Map.class))).thenReturn(streamBlob);

    Iterable<HashAlgorithm> hashing = Collections.singletonList(HashAlgorithm.SHA1);

    TempBlob result = underTest.ingest(tempFile, "text/plain", hashing, false);

    assertThat(result, is(notNullValue()));
    assertThat(result.getBlob(), is(streamBlob));
    // both create methods should have been called
    verify(blobStore).create(any(Path.class), any(Map.class), anyLong(), any(HashCode.class));
    verify(blobStore).create(any(InputStream.class), any(Map.class));
  }

  @Test
  public void testIngestSrcBlobSameStorePassesContentTypeFromHeaders() {
    Blob srcBlob = mock(Blob.class);
    BlobId srcBlobId = new BlobId("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    when(srcBlob.getId()).thenReturn(srcBlobId);
    Map<String, String> srcHeaders = Map.of(
        CONTENT_TYPE_HEADER, "application/json",
        BLOB_NAME_HEADER, "original-blob");
    when(srcBlob.getHeaders()).thenReturn(srcHeaders);

    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn("shared-store");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);

    BlobStore srcStore = mock(BlobStore.class);
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    when(srcConfig.getName()).thenReturn("shared-store");
    when(srcStore.getBlobStoreConfiguration()).thenReturn(srcConfig);

    Map<HashAlgorithm, HashCode> hashes =
        Map.of(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    TempBlob result = underTest.ingest(srcBlob, srcStore, hashes);

    assertThat(result, is(notNullValue()));
    assertThat(result, is(instanceOf(AttachableBlob.class)));
    // verify copy was NOT called since we re-attach instead
    verify(blobStore, never()).copy(any(BlobId.class), any(Map.class));
  }
}
