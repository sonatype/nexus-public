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
package org.sonatype.nexus.content.maven.internal.index;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.view.Content;

import org.apache.maven.index.reader.Record;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FluentAsset#download() costs a storage request per call and throws MissingBlobException for a
 * missing blob. The index publisher only needs the artifact's last-modified time, so it must read
 * it from metadata instead of downloading every artifact: the blob creation time for a hosted
 * asset, the stored upstream Last-Modified for a proxy asset.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenContentIndexPublisherTest
{
  private static final OffsetDateTime BLOB_CREATED = OffsetDateTime.of(2024, 3, 5, 10, 20, 30, 123_000_000,
      ZoneOffset.UTC);

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private FluentAsset asset;

  @Mock
  private FluentComponent component;

  @Mock
  private AssetBlob assetBlob;

  private final MavenContentIndexPublisher underTest = new MavenContentIndexPublisher(1000);

  @Before
  public void setUp() {
    when(mavenContentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(mavenContentFacet.exists(any(MavenPath.class))).thenReturn(false);

    when(component.namespace()).thenReturn("org.foo");
    when(component.name()).thenReturn("bar");
    when(component.attributes(Maven2Format.NAME)).thenReturn(new NestedAttributesMap("maven2", new HashMap<>()));

    when(assetBlob.blobCreated()).thenReturn(BLOB_CREATED);
    when(assetBlob.blobSize()).thenReturn(123L);
    when(assetBlob.checksums()).thenReturn(Map.of("SHA1", "abc123"));

    when(asset.path()).thenReturn("/org/foo/bar/1.0/bar-1.0.jar");
    when(asset.component()).thenReturn(Optional.of(component));
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(asset.attributes(Maven2Format.NAME)).thenReturn(new NestedAttributesMap("maven2", new HashMap<>()));
    when(asset.attributes()).thenReturn(new NestedAttributesMap("attributes", new HashMap<>()));

    // a missing blob: any attempt to download() it throws
    BlobRef missingBlobRef = mock(BlobRef.class);
    MissingBlobException missingBlobException = new MissingBlobException(missingBlobRef);
    when(asset.download()).thenThrow(missingBlobException);
  }

  @Test
  public void toRecord_fileModified_isBlobCreationTime() {
    Record record = underTest.toRecord(asset, mavenContentFacet, true);

    assertThat(record.get(Record.FILE_MODIFIED), is(BLOB_CREATED.toInstant().toEpochMilli()));
  }

  @Test
  public void toRecord_doesNotDownloadAssetContent() {
    // download() is stubbed to throw MissingBlobException, so this would fail if it were called
    underTest.toRecord(asset, mavenContentFacet, true);
    underTest.toRecord(asset, mavenContentFacet, false);

    verify(asset, never()).download();
  }

  @Test
  public void toRecord_proxyAsset_usesStoredUpstreamLastModified() {
    stubContentAttributes(Map.of(Content.CONTENT_LAST_MODIFIED, "2020-01-02T03:04:05.000Z"));

    Record record = underTest.toRecord(asset, mavenContentFacet, false);

    assertThat(record.get(Record.FILE_MODIFIED), is(Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()));
  }

  @Test
  public void toRecord_proxyAsset_withoutUpstreamLastModified_omitsFileModified() {
    stubContentAttributes(Map.of());

    Record record = underTest.toRecord(asset, mavenContentFacet, false);

    assertThat(record.get(Record.FILE_MODIFIED), is(nullValue()));
  }

  @Test
  public void toRecord_proxyAsset_unparseableUpstreamLastModified_omitsFileModified() {
    stubContentAttributes(Map.of(Content.CONTENT_LAST_MODIFIED, "not a date"));

    Record record = underTest.toRecord(asset, mavenContentFacet, false);

    assertThat(record.get(Record.FILE_MODIFIED), is(nullValue()));
  }

  @Test
  public void toRecord_proxyAssetWithoutStoredContentHeaders_usesBlobCreationTime() {
    // no "content" attributes: FluentAsset#download() would fall back to the blob creation time
    Record record = underTest.toRecord(asset, mavenContentFacet, false);

    assertThat(record.get(Record.FILE_MODIFIED), is(BLOB_CREATED.toInstant().toEpochMilli()));
  }

  private void stubContentAttributes(final Map<String, Object> contentHeaders) {
    NestedAttributesMap content = new NestedAttributesMap(Content.CONTENT, new HashMap<>(contentHeaders));
    NestedAttributesMap all = new NestedAttributesMap("attributes", new HashMap<>());
    all.set(Content.CONTENT, content.backing());
    when(asset.attributes()).thenReturn(all);
    when(asset.attributes(Content.CONTENT)).thenReturn(content);
  }
}
