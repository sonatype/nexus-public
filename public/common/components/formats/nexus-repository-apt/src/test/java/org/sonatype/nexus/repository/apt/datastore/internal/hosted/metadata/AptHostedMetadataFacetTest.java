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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.AptContentFacetImpl;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.store.AptAssetStore;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_NAME;

/**
 * Tests for {@link AptHostedMetadataFacet}, focusing on the package index building logic.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AptHostedMetadataFacetTest

{
  private static final String REPO_NAME = "apt-hosted-test";

  private static final String DISTRIBUTION = "focal";

  @Mock
  private Repository repository;

  @Mock
  private AptContentFacetImpl contentFacet;

  @Mock
  private AptKeyValueFacet keyValueFacet;

  @Mock
  private AptSigningFacet signingFacet;

  @Mock
  private Clock clock;

  @Mock
  private EventManager eventManager;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder fluentAssetBuilder;

  @Mock
  private FluentAsset releaseAsset;

  @Mock
  private org.sonatype.nexus.repository.content.AssetBlob assetBlob;

  @Mock
  private AptAssetStore assetStore;

  @Mock
  private FormatStoreManager formatStoreManager;

  private ObjectMapper objectMapper;

  private AptHostedMetadataFacet underTest;

  @Before
  public void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    Cooperation2Factory cooperationFactory = new DefaultCooperation2Factory();

    // Setup repository
    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(AptKeyValueFacet.class)).thenReturn(keyValueFacet);
    when(repository.facet(AptSigningFacet.class)).thenReturn(signingFacet);

    // Setup content facet
    when(contentFacet.getDistribution()).thenReturn(DISTRIBUTION);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacet.contentRepositoryId()).thenReturn(1);
    when(fluentAssets.path(anyString())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());

    // Setup stores and asset store
    when(formatStoreManager.assetStore(anyString())).thenReturn(assetStore);
    ContentFacetStores stores = new ContentFacetStores(mock(), "test-blobstore", formatStoreManager, "apt");
    when(contentFacet.stores()).thenReturn(stores);

    Continuation<Asset> emptyAssets = mock(Continuation.class);
    when(emptyAssets.isEmpty()).thenReturn(true);
    when(assetStore.browsePackageIndexAssets(any(Integer.class), any(Integer.class), any(), anyString()))
        .thenReturn(emptyAssets);

    // Setup signing facet
    when(signingFacet.signInline(anyString())).thenReturn("signed-inline".getBytes());
    when(signingFacet.signExternal(anyString())).thenReturn("signed-external".getBytes());

    // Setup clock
    when(clock.clusterTime()).thenReturn(OffsetDateTime.now());

    // Create facet
    underTest = new AptHostedMetadataFacet(
        objectMapper,
        clock,
        cooperationFactory,
        true, // cooperation enabled
        Duration.ZERO, // majorTimeout
        Duration.ofSeconds(30), // minorTimeout
        100); // threadsPerKey

    // Initialize facet
    underTest.installDependencies(eventManager);
    underTest.attach(repository);
    underTest.init();
  }

  @Test
  public void testRebuildMetadata_WithMultipleArchitectures_CreatesIndexesPerArchitecture() throws Exception {
    // Setup: packages from two different architectures
    Map<String, Object> amd64Package1 = createPackageMetadata("hello", "amd64", "Package: hello\nVersion: 1.0");
    Map<String, Object> amd64Package2 = createPackageMetadata("world", "amd64", "Package: world\nVersion: 2.0");
    Map<String, Object> arm64Package = createPackageMetadata("arm-tool", "arm64", "Package: arm-tool\nVersion: 3.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(
            objectMapper.writeValueAsString(amd64Package1),
            objectMapper.writeValueAsString(amd64Package2),
            objectMapper.writeValueAsString(arm64Package)));

    setupMockPutOperations();

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Package indexes created for both architectures
    assertThat(result).isPresent();

    // Should create 3 files per architecture (plain, gz, bz2) = 6 total Package files
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentFacet, times(9)).put(pathCaptor.capture(), any()); // 6 Package files + 3 Release files

    // Verify amd64 indexes were created
    assertThat(pathCaptor.getAllValues())
        .as("Should create binary-amd64 indexes")
        .anyMatch(p -> p.contains("binary-amd64"));
    // Verify arm64 indexes were created
    assertThat(pathCaptor.getAllValues())
        .as("Should create binary-arm64 indexes")
        .anyMatch(p -> p.contains("binary-arm64"));
  }

  @Test
  public void testRebuildMetadata_WithDuplicatePackages_DeduplicatesCorrectly() throws Exception {
    // Setup: Multiple versions of the same package - all versions should be included
    Map<String, Object> helloV1 = createPackageMetadata("hello", "amd64", "Package: hello\nVersion: 1.0");
    Map<String, Object> helloV2 = createPackageMetadata("hello", "amd64", "Package: hello\nVersion: 2.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(
            objectMapper.writeValueAsString(helloV1),
            objectMapper.writeValueAsString(helloV2) // Both versions should be included
        ));

    // Capture the content written to the Packages file
    final Map<String, String> capturedContent = new HashMap<>();
    when(contentFacet.put(anyString(), any())).thenAnswer(invocation -> {
      String path = invocation.getArgument(0);
      Payload payload = invocation.getArgument(1);

      // Capture content from StreamPayload for Package files
      if (path.contains("binary-amd64/Packages") && !path.endsWith(".gz") && !path.endsWith(".bz2")) {
        if (payload instanceof StreamPayload streamPayload) {
          try (InputStream is = streamPayload.openInputStream()) {
            capturedContent.put(path, new String(is.readAllBytes(), StandardCharsets.UTF_8));
          }
        }
      }

      // Setup AssetBlob with checksums for Release file generation
      Map<String, String> checksums = new HashMap<>();
      checksums.put("MD5", "d41d8cd98f00b204e9800998ecf8427e");
      checksums.put("SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
      when(assetBlob.checksums()).thenReturn(checksums);
      when(assetBlob.blobSize()).thenReturn(1024L);
      when(releaseAsset.blob()).thenReturn(Optional.of(assetBlob));
      when(releaseAsset.download()).thenReturn(new Content(new BytesPayload("test".getBytes(), "text/plain")));

      return releaseAsset;
    });

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: All package versions are included in the index files
    assertThat(result).isPresent();

    // Should create 3 Package files (plain, gz, bz2) + 3 Release files = 6 total
    verify(contentFacet, times(6)).put(anyString(), any());

    // Verify: Both versions are present in the Package file content
    assertThat(capturedContent)
        .as("Should have captured Package file content")
        .hasSize(1);
    String packageContent = capturedContent.values().iterator().next();
    assertThat(packageContent)
        .as("Package file should contain both versions")
        .contains("Version: 1.0", "Version: 2.0");
  }

  @Test
  public void testRebuildMetadata_WithEmptyKVStore_ReturnsNull() throws Exception {
    // Setup: No packages in KV store
    when(keyValueFacet.browsePackagesMetadata()).thenReturn(Stream.empty());

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: No release file created when no packages exist
    assertThat(result).isEmpty();

    // Verify: deleteAssetsByPrefix called to clean up old metadata
    verify(contentFacet).deleteAssetsByPrefix(anyString());
  }

  @Test
  public void testRebuildMetadata_WithSingleArchitecture_CreatesIndexForThatArchOnly() throws Exception {
    // Setup: Only amd64 packages
    Map<String, Object> pkg1 = createPackageMetadata("pkg1", "amd64", "Package: pkg1\nVersion: 1.0");
    Map<String, Object> pkg2 = createPackageMetadata("pkg2", "amd64", "Package: pkg2\nVersion: 2.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(
            objectMapper.writeValueAsString(pkg1),
            objectMapper.writeValueAsString(pkg2)));

    setupMockPutOperations();

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Indexes only for amd64
    assertThat(result).isPresent();

    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentFacet, times(6)).put(pathCaptor.capture(), any());

    // Should have amd64 but not arm64
    assertThat(pathCaptor.getAllValues())
        .as("Should create binary-amd64 indexes")
        .anyMatch(p -> p.contains("binary-amd64"))
        .as("Should not create binary-arm64 indexes")
        .noneMatch(p -> p.contains("binary-arm64"));
  }

  @Test
  public void testRebuildMetadata_WithAllArchitecturesValue_HandlesCorrectly() throws Exception {
    // Setup: Package with "all" architecture (arch-independent)
    Map<String, Object> allPackage = createPackageMetadata("docs", "all", "Package: docs\nVersion: 1.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(objectMapper.writeValueAsString(allPackage)));

    setupMockPutOperations();

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Index created for "all" architecture
    assertThat(result).isPresent();

    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentFacet, times(6)).put(pathCaptor.capture(), any());

    assertThat(pathCaptor.getAllValues())
        .as("Should create binary-all indexes")
        .anyMatch(p -> p.contains("binary-all"));
  }

  @Test
  public void testRebuildMetadata_CleansUpOldMetadataFirst() throws Exception {
    // Setup: Single package with no existing stale architectures
    Map<String, Object> pkg = createPackageMetadata("test", "amd64", "Package: test\nVersion: 1.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(objectMapper.writeValueAsString(pkg)));

    setupMockPutOperations();

    // Execute
    underTest.rebuildMetadata();

    // Verify: browsePackageIndexAssets is called to check for stale architectures
    verify(assetStore).browsePackageIndexAssets(eq(1), eq(1000), eq(null),
        eq("/dists/" + DISTRIBUTION + "/main/binary-%"));
  }

  @Test
  public void testRebuildMetadata_CreatesAllReleaseFiles() throws Exception {
    // Setup: Single package
    Map<String, Object> pkg = createPackageMetadata("test", "amd64", "Package: test\nVersion: 1.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(objectMapper.writeValueAsString(pkg)));

    setupMockPutOperations();

    // Execute
    underTest.rebuildMetadata();

    // Verify: All three release files are created
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentFacet, times(6)).put(pathCaptor.capture(), any());

    // Should create: Release, InRelease, Release.gpg
    assertThat(pathCaptor.getAllValues())
        .as("Should create all three release files")
        .anyMatch(p -> p.endsWith("/Release"))
        .anyMatch(p -> p.endsWith("/InRelease"))
        .anyMatch(p -> p.endsWith("/Release.gpg"));
  }

  @Test
  public void testRebuildMetadata_CreatesIndexesForAllArchitectures() throws Exception {
    // Setup: Multiple architectures
    Map<String, Object> amd64 = createPackageMetadata("p1", "amd64", "Package: p1");
    Map<String, Object> arm64 = createPackageMetadata("p2", "arm64", "Package: p2");
    Map<String, Object> all = createPackageMetadata("p3", "all", "Package: p3");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(
            objectMapper.writeValueAsString(amd64),
            objectMapper.writeValueAsString(arm64),
            objectMapper.writeValueAsString(all)));

    setupMockPutOperations();

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Indexes created for all three architectures
    assertThat(result).isPresent();

    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentFacet, times(12)).put(pathCaptor.capture(), any());

    // Should create indexes for all three architectures
    assertThat(pathCaptor.getAllValues())
        .as("Should create indexes for all three architectures")
        .anyMatch(p -> p.contains("binary-amd64"))
        .anyMatch(p -> p.contains("binary-arm64"))
        .anyMatch(p -> p.contains("binary-all"));
  }

  @Test
  public void testUsesCorrectCooperationScope() throws Exception {
    Cooperation2Factory testCooperationFactory = mock(Cooperation2Factory.class);
    Cooperation2Factory.Builder testBuilder = mock(Cooperation2Factory.Builder.class);

    when(testCooperationFactory.configure()).thenReturn(testBuilder);
    when(testBuilder.enabled(true)).thenReturn(testBuilder);
    when(testBuilder.majorTimeout(any())).thenReturn(testBuilder);
    when(testBuilder.minorTimeout(any())).thenReturn(testBuilder);
    when(testBuilder.threadsPerKey(anyInt())).thenReturn(testBuilder);
    when(testBuilder.build(anyString())).thenReturn(mock(Cooperation2.class));

    AptHostedMetadataFacet testFacet = new AptHostedMetadataFacet(
        objectMapper,
        clock,
        testCooperationFactory,
        true,
        Duration.ZERO,
        Duration.ofSeconds(30),
        100);

    testFacet.installDependencies(eventManager);
    testFacet.attach(repository);
    testFacet.init();

    verify(testBuilder).build(REPO_NAME + ":apt-metadata");
  }

  @Test
  public void testRebuildMetadata_RemovesStaleArchitectureMetadata() throws Exception {
    // Setup: KV store has packages for amd64 and arm64 only
    Map<String, Object> amd64Package = createPackageMetadata("pkg1", "amd64", "Package: pkg1\nVersion: 1.0");
    Map<String, Object> arm64Package = createPackageMetadata("pkg2", "arm64", "Package: pkg2\nVersion: 2.0");

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(Stream.of(
            objectMapper.writeValueAsString(amd64Package),
            objectMapper.writeValueAsString(arm64Package)));

    // Setup: Existing Package index assets include amd64, arm64, AND i386 (stale)
    Asset amd64Asset = mock(Asset.class);
    when(amd64Asset.path()).thenReturn("/dists/" + DISTRIBUTION + "/main/binary-amd64/Packages");

    Asset arm64Asset = mock(Asset.class);
    when(arm64Asset.path()).thenReturn("/dists/" + DISTRIBUTION + "/main/binary-arm64/Packages.gz");

    Asset i386Asset = mock(Asset.class);
    when(i386Asset.path()).thenReturn("/dists/" + DISTRIBUTION + "/main/binary-i386/Packages.bz2");

    Continuation<Asset> assetContinuation = mock(Continuation.class);
    List<Asset> assetList = List.of(amd64Asset, arm64Asset, i386Asset);
    when(assetContinuation.iterator()).thenReturn(assetList.iterator());
    when(assetContinuation.stream()).thenReturn(assetList.stream());

    when(assetStore.browsePackageIndexAssets(any(Integer.class), any(Integer.class), any(), anyString()))
        .thenReturn(assetContinuation);

    setupMockPutOperations();

    // Execute
    underTest.rebuildMetadata();

    // Verify: deleteAssetsByPrefix called for stale i386, but NOT for current amd64/arm64
    verify(contentFacet).deleteAssetsByPrefix("/dists/" + DISTRIBUTION + "/main/binary-i386/");
    verify(contentFacet, times(0)).deleteAssetsByPrefix("/dists/" + DISTRIBUTION + "/main/binary-amd64/");
    verify(contentFacet, times(0)).deleteAssetsByPrefix("/dists/" + DISTRIBUTION + "/main/binary-arm64/");
  }

  private Map<String, Object> createPackageMetadata(
      final String packageName,
      final String architecture,
      final String indexSection)
  {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(P_PACKAGE_NAME, packageName);
    metadata.put(P_ARCHITECTURE, architecture);
    metadata.put(P_INDEX_SECTION, indexSection);
    return metadata;
  }

  private void setupMockPutOperations() throws IOException {
    // Setup AssetBlob with checksums
    Map<String, String> checksums = new HashMap<>();
    checksums.put("MD5", "d41d8cd98f00b204e9800998ecf8427e");
    checksums.put("SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

    when(assetBlob.checksums()).thenReturn(checksums);
    when(assetBlob.blobSize()).thenReturn(1024L);

    // Setup FluentAsset to return the blob
    when(releaseAsset.blob()).thenReturn(Optional.of(assetBlob));
    when(releaseAsset.download()).thenReturn(new Content(new BytesPayload("test".getBytes(), "text/plain")));

    // Setup contentFacet.put() to return the mocked asset
    when(contentFacet.put(anyString(), any())).thenReturn(releaseAsset);
  }
}
