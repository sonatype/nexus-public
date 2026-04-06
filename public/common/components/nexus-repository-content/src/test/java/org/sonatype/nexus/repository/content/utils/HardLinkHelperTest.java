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
package org.sonatype.nexus.repository.content.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;

public class HardLinkHelperTest
    extends TestSupport
{
  @Rule
  public org.junit.rules.TemporaryFolder temporaryFolder = new org.junit.rules.TemporaryFolder();

  @Mock
  private MimeSupport mimeSupport;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentBlobs fluentBlobs;

  @Mock
  private Blob blob;

  private HashCode hashCode;

  private HardLinkHelper underTest;

  @Before
  public void setUp() throws Exception {
    hashCode = HashCode.fromString("aabb1122");
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.blobs()).thenReturn(fluentBlobs);
    when(fluentBlobs.ingest(any(Path.class), anyMap(), any(HashCode.class), anyLong())).thenReturn(blob);
    when(mimeSupport.detectMimeType(any(InputStream.class), anyString())).thenReturn("application/octet-stream");

    underTest = new HardLinkHelper(mimeSupport);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorNullMimeSupportRejected() {
    new HardLinkHelper(null);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIngestHardLinkAutoDetectsMimeType() throws Exception {
    File tempFile = temporaryFolder.newFile("test-file.bin");

    Blob result = underTest.ingestHardLink(repository, tempFile, hashCode);

    assertThat(result, is(notNullValue()));
    verify(mimeSupport).detectMimeType(any(InputStream.class), eq(tempFile.getPath()));

    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fluentBlobs).ingest(eq(tempFile.toPath()), headersCaptor.capture(), eq(hashCode), anyLong());

    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(CONTENT_TYPE_HEADER), is(equalTo("application/octet-stream")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIngestHardLinkWithProvidedContentType() throws Exception {
    File tempFile = temporaryFolder.newFile("test-file.txt");
    String providedContentType = "text/plain";

    Blob result = underTest.ingestHardLink(repository, tempFile, hashCode, providedContentType);

    assertThat(result, is(notNullValue()));
    verify(mimeSupport, never()).detectMimeType(any(InputStream.class), anyString());

    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fluentBlobs).ingest(eq(tempFile.toPath()), headersCaptor.capture(), eq(hashCode), anyLong());

    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(CONTENT_TYPE_HEADER), is(equalTo(providedContentType)));
  }

  @Test(expected = NullPointerException.class)
  public void testIngestHardLinkNullRepositoryRejected() throws Exception {
    File tempFile = temporaryFolder.newFile("test-file.bin");
    underTest.ingestHardLink(null, tempFile, hashCode);
  }

  @Test(expected = NullPointerException.class)
  public void testIngestHardLinkNullContentRejected() throws Exception {
    underTest.ingestHardLink(repository, null, hashCode);
  }

  @Test(expected = NullPointerException.class)
  public void testIngestHardLinkNullHashCodeRejected() throws Exception {
    File tempFile = temporaryFolder.newFile("test-file.bin");
    underTest.ingestHardLink(repository, tempFile, null);
  }

  @Test(expected = NullPointerException.class)
  public void testIngestHardLinkNullContentTypeRejected() throws Exception {
    File tempFile = temporaryFolder.newFile("test-file.bin");
    underTest.ingestHardLink(repository, tempFile, hashCode, null);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIngestHardLinkSetsCorrectHeaders() throws Exception {
    File tempFile = temporaryFolder.newFile("my-artifact.jar");
    String contentType = "application/java-archive";

    underTest.ingestHardLink(repository, tempFile, hashCode, contentType);

    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fluentBlobs).ingest(eq(tempFile.toPath()), headersCaptor.capture(), eq(hashCode), anyLong());

    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get(BLOB_NAME_HEADER), is(equalTo(tempFile.getPath())));
    assertThat(capturedHeaders.get(CONTENT_TYPE_HEADER), is(equalTo(contentType)));
  }
}
