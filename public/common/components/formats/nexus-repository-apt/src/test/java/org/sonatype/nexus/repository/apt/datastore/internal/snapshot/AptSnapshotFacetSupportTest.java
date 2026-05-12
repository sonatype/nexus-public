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
package org.sonatype.nexus.repository.apt.datastore.internal.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.internal.debian.Release;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotComponentSelector;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AptSnapshotFacetSupportTest
{
  @Mock
  private Repository repository;

  @Mock
  private AptContentFacet contentFacet;

  @Mock
  private SnapshotComponentSelector selector;

  @Mock
  private TempBlob tempBlob;

  @Captor
  private ArgumentCaptor<String> pathCaptor;

  private TestableAptSnapshotFacetSupport underTest;

  @BeforeEach
  public void setUp() throws Exception {
    underTest = new TestableAptSnapshotFacetSupport();
    underTest.attach(repository);

    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.getTempBlob(any(InputStream.class), anyString())).thenReturn(tempBlob);
  }

  @Test
  public void testCreateSnapshot_FlatRepo_NoByHash() throws IOException {
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("focal");

    underTest.createSnapshot("test-snapshot", selector);

    // Verify that assets are created
    verify(contentFacet, atLeastOnce()).findOrCreateMetadataAsset(eq(tempBlob), pathCaptor.capture());

    List<String> capturedPaths = pathCaptor.getAllValues();

    // Verify we have release files
    assertThat("Should have Release file", capturedPaths, hasItem(containsString("/Release")));

    // Verify we have package files
    assertThat("Should have Packages files", capturedPaths, hasItem(containsString("Packages")));

    // Verify no by-hash paths for flat repos
    assertThat("Flat repos should not have by-hash items", capturedPaths, not(hasItem(containsString("by-hash"))));
  }

  @Test
  public void testCreateSnapshot_NonFlatRepo_WithByHash() throws IOException {
    when(contentFacet.isFlat()).thenReturn(false);
    when(contentFacet.getDistribution()).thenReturn("focal");
    when(selector.getArchitectures(any(Release.class))).thenReturn(Arrays.asList("amd64", "arm64"));
    when(selector.getComponents(any(Release.class))).thenReturn(Arrays.asList("main", "universe"));

    underTest.createSnapshot("test-snapshot", selector);

    verify(contentFacet, atLeastOnce()).findOrCreateMetadataAsset(eq(tempBlob), pathCaptor.capture());

    List<String> capturedPaths = pathCaptor.getAllValues();

    // Verify paths for different architectures and components
    assertThat("Should have amd64 main paths", capturedPaths, hasItem(containsString("main/binary-amd64")));
    assertThat("Should have arm64 universe paths", capturedPaths, hasItem(containsString("universe/binary-arm64")));
    assertThat("Should have amd64 universe paths", capturedPaths, hasItem(containsString("universe/binary-amd64")));
    assertThat("Should have arm64 main paths", capturedPaths, hasItem(containsString("main/binary-arm64")));

    // Verify by-hash paths exist for multiple combinations
    assertThat("Should have by-hash paths for multi-arch/component setup", capturedPaths,
        hasItem(containsString("by-hash")));

    // Verify specific hash algorithms are present
    assertThat("Should have SHA256 by-hash paths", capturedPaths, hasItem(containsString("by-hash/SHA256")));
    assertThat("Should have MD5 by-hash paths", capturedPaths, hasItem(containsString("by-hash/MD5")));
  }

  @Test
  public void testCreateSnapshot_AssetPathFormat() throws IOException {
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("focal");

    underTest.createSnapshot("my-snapshot-id", selector);

    verify(contentFacet, atLeastOnce()).findOrCreateMetadataAsset(eq(tempBlob), pathCaptor.capture());

    List<String> capturedPaths = pathCaptor.getAllValues();

    // Verify all paths start with the snapshot prefix
    capturedPaths.forEach(
        path -> assertThat("Path should start with snapshot prefix", path, startsWith("/snapshots/my-snapshot-id/")));
  }

  private static class TestableAptSnapshotFacetSupport
      extends AptSnapshotFacetSupport
  {
    @Override
    protected List<SnapshotItem> fetchSnapshotItems(List<ContentSpecifier> specs) throws IOException {
      return specs.stream().map(spec -> {
        Content content;

        if (spec.role == SnapshotItem.Role.RELEASE_INDEX) {
          // Create release content with package file checksums
          String releaseContent = "SHA256:\n" +
              " abc123456789 12345 main/binary-amd64/Packages\n" +
              " def456789012 6789 main/binary-amd64/Packages.gz\n" +
              " ghi789012345 1234 main/binary-amd64/Packages.bz2\n" +
              " jkl012345678 5678 main/binary-amd64/Packages.xz\n";
          content = new Content(new StringPayload(releaseContent, "text/plain"));
        }
        else if (spec.role.name().startsWith("PACKAGE_INDEX") && !spec.path.contains("by-hash")) {
          // Create package content with checksums in asset attributes
          content = new Content(new StringPayload("Package content for " + spec.path, "text/plain"));

          Asset asset = mock(Asset.class);
          AssetBlob assetBlob = mock(AssetBlob.class);
          Map<String, String> checksums = new HashMap<>();
          checksums.put("SHA256", "abc123456789");
          checksums.put("MD5", "def456789012");

          // Use lenient stubbing since these mocks are only used when by-hash is enabled (non-flat repos)
          lenient().when(asset.hasBlob()).thenReturn(true);
          lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
          lenient().when(assetBlob.checksums()).thenReturn(checksums);
          content.getAttributes().set(Asset.class, asset);
        }
        else {
          // Default content for other types including by-hash
          content = new Content(new StringPayload("Content for " + spec.path, "text/plain"));
        }

        return new SnapshotItem(spec, content);
      }).toList();
    }
  }
}
