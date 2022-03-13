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
package org.sonatype.nexus.repository.replication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;

public class ReplicationIngesterSupportTest
    extends TestSupport
{
  private TestReplicationIngester underTest;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private ReplicationIngesterHelper replicationIngesterHelper;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Blob blob;

  @Mock
  private BlobAttributes blobAttributes;

  private Map<String, String> blobHeaders;

  private Properties properties;

  @Before
  public void setup() {
    when(blobAttributes.getProperties()).thenReturn(getProperties());
    when(blobStoreManager.get(anyString())).thenReturn(blobStore);
    when(blobStore.get(any(BlobId.class))).thenReturn(blob);
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(blobAttributes);
    when(blobAttributes.getHeaders()).thenReturn(getHeaders());
    underTest = new TestReplicationIngester(blobStoreManager, replicationIngesterHelper);
  }

  @Test
  public void testExtractAttributeFromProperties_extractsExpectedProperties() {
    Map<String, Object> extractedProperties = underTest.extractAssetAttributesFromProperties(getProperties());
    verifyExtractedProperties(extractedProperties);
  }

  @Test(expected = ReplicationIngestionException.class)
  public void testIngestBlob_failsIfBlobstoreNotPresent() {
    when(blobStoreManager.get(anyString())).thenReturn(null);
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.ADDED);
  }

  @Test(expected = ReplicationIngestionException.class)
  public void testIngestBlob_failsIfBlobAttributesNotFound() {
    when(blobStore.getBlobAttributes(any(BlobId.class))).thenReturn(null);
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.ADDED);
  }

  @Test(expected = ReplicationIngestionException.class)
  public void testIngestBlob_failsIfBlobNotFound() {
    when(blobStore.get(any(BlobId.class))).thenReturn(null);
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.ADDED);
  }

  @Test
  public void testIngestBlob_callsDeleteIfDeleteEvent() {
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.DELETED);
    verify(replicationIngesterHelper, times(1)).deleteReplication("blobName", "repositoryName");
  }

  @Test
  public void testIngestBlob_callsReplicateIfAddEvent() throws IOException {
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.ADDED);
    verify(replicationIngesterHelper, times(1))
        .replicate(any(String.class), any(Blob.class), any(Map.class), any(Map.class), any(String.class), any(String.class));
  }

  @Test
  public void testIngestBlob_callsReplicateIfUpdateEvent() throws IOException {
    underTest.ingestBlob("blobId", "blobStoreId", "repositoryName", BlobEventType.UPDATED);
    verify(replicationIngesterHelper, times(1))
        .replicate(any(String.class), any(Blob.class), any(Map.class), any(Map.class), any(String.class), any(String.class));
  }

  private void verifyExtractedProperties(final Map<String, Object> extractedProperties) {
    assertThat(extractedProperties.size(), is(3));
    assertThat(((Map<String, Object>) extractedProperties.get("checksum")).size(), is(4));
    assertThat(((Map<String, Object>) extractedProperties.get("content")).size(), is(1));
    assertThat(((Map<String, Object>) extractedProperties.get("provenance")).size(), is(1));
    assertThat(((Map<String, Object>) extractedProperties.get("checksum")).get("md5").toString(), is("md5hash"));
    assertThat(((Map<String, Object>) extractedProperties.get("checksum")).get("sha256").toString(), is("sha256hash"));
    assertThat(((Map<String, Object>) extractedProperties.get("checksum")).get("sha1").toString(), is("sha1hash"));
    assertThat(((Map<String, Object>) extractedProperties.get("checksum")).get("sha512").toString(), is("sha512hash"));
    assertThat(((Map<String, Object>) extractedProperties.get("content")).get("last_modified").toString(),
        is("Mon May 03 21:32:25 COT 2021"));
    assertThat(((Map<String, Object>) extractedProperties.get("provenance")).get("hashes_not_verified").toString(),
        is("false"));
  }

  private Map<String, String> getHeaders() {
    if (blobHeaders == null) {
      blobHeaders = new HashMap<>();
      blobHeaders.put(BLOB_NAME_HEADER, "blobName");
    }
    return blobHeaders;
  }

  private Properties getProperties() {
    if (properties == null) {
      properties = new Properties();
      properties.put("@attributes.asset.checksum.md5", "md5hash");
      properties.put("@attributes.asset.checksum.sha256", "sha256hash");
      properties.put("@attributes.asset.checksum.sha1", "sha1hash");
      properties.put("@attributes.asset.checksum.sha512", "sha512hash");
      properties.put("@attributes.asset.content.last_modified", "Mon May 03 21:32:25 COT 2021");
      properties.put("@attributes.asset.provenance.hashes_not_verified", "false");
    }
    return properties;
  }

  private static class TestReplicationIngester extends ReplicationIngesterSupport {
    @Override
    public String getFormat() {
      return "TEST";
    }

    public TestReplicationIngester(final BlobStoreManager blobstoreManager,
                                   final ReplicationIngesterHelper replicationIngesterHelper)
    {
      super(blobstoreManager, replicationIngesterHelper);
    }
  }
}
