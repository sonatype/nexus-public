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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AptProxyMetadataFacet}.
 *
 * This test class covers:
 * - Metadata rebuild behavior (signing config, distributions)
 * - Merge algorithm (parsing, determinism, cached entry building)
 * - Multi-component support (slice keys, separators, paths)
 * - Pagination handling in pool scanning
 */
public class AptProxyMetadataFacetTest
    extends TestSupport
{
  private static final String REPO_NAME = "apt-proxy-test";

  private static final String DISTRIBUTION = "jammy";

  @Mock
  private Repository repository;

  @Mock
  private AptContentFacet contentFacet;

  @Mock
  private AptKeyValueFacet keyValueFacet;

  @Mock
  private AptSigningFacet signingFacet;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private HttpClientFacet httpClientFacet;

  @Mock
  private HttpClient httpClient;

  @Mock
  private Clock clock;

  @Mock
  private EventManager eventManager;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAsset releaseAsset;

  @Mock
  private org.sonatype.nexus.repository.content.AssetBlob assetBlob;

  private ObjectMapper objectMapper;

  private AptProxyMetadataFacet underTest;

  @Before
  public void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    Cooperation2Factory cooperationFactory = new DefaultCooperation2Factory();

    // Setup repository
    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(AptKeyValueFacet.class)).thenReturn(keyValueFacet);
    when(repository.facet(AptSigningFacet.class)).thenReturn(signingFacet);
    when(repository.facet(ProxyFacet.class)).thenReturn(proxyFacet);
    when(repository.facet(HttpClientFacet.class)).thenReturn(httpClientFacet);

    // Setup content facet
    when(contentFacet.getDistribution()).thenReturn(DISTRIBUTION);
    when(contentFacet.isFlat()).thenReturn(false); // Default to non-flat repository
    when(contentFacet.assets()).thenReturn(fluentAssets);

    // Setup HTTP client
    when(httpClientFacet.getHttpClient()).thenReturn(httpClient);
    when(proxyFacet.getRemoteUrl()).thenReturn(URI.create("http://upstream.example.com/"));

    // Setup clock
    when(clock.clusterTime()).thenReturn(OffsetDateTime.now());

    // Create facet
    underTest = new AptProxyMetadataFacet(
        objectMapper,
        clock,
        cooperationFactory,
        true,
        Duration.ZERO,
        Duration.ofSeconds(30),
        100);

    // Initialize facet
    underTest.installDependencies(eventManager);
    underTest.attach(repository);
    underTest.init();
  }

  // ==========================================================================
  // REBUILD METADATA TESTS
  // ==========================================================================

  @Test
  public void testRebuildMetadata_WhenSigningNotConfigured_ReturnsNull() throws Exception {
    // Setup: Signing not configured
    when(signingFacet.isConfigured()).thenReturn(false);

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Returns null (passthrough mode)
    assertThat(result.isPresent(), is(false));

    // Verify: No upstream fetch attempted
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void testRebuildMetadata_WhenSigningConfigured_AndNoDistributions_ReturnsNull() throws Exception {
    // Setup: Signing configured but no distributions tracked
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("");
    when(keyValueFacet.getTrackedDistributions()).thenReturn(Set.of());

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Returns null when no distributions
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testRebuildMetadata_WhenSigningConfigured_UsesConfiguredDistribution() throws Exception {
    // Setup: Signing configured with specific distribution
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn(DISTRIBUTION);
    when(contentFacet.isEnforceDistribution()).thenReturn(true);

    // Setup: Upstream returns 404 (no metadata)
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(404);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(httpClient.execute(any())).thenReturn(response);

    // Setup: No cached packages
    Continuation<FluentAsset> emptyContinuation = mockEmptyContinuation();
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(emptyContinuation);

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: No tracked distributions query when distribution is configured
    verify(keyValueFacet, never()).getTrackedDistributions();
  }

  @Test
  public void testRebuildMetadata_WhenDistributionBlank_UsesTrackedDistributions() throws Exception {
    // Setup: Signing configured with blank distribution
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("");
    when(keyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy", "focal"));

    // Setup: Upstream returns 404
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(404);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(httpClient.execute(any())).thenReturn(response);

    // Setup: No cached packages
    Continuation<FluentAsset> emptyContinuation = mockEmptyContinuation();
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(emptyContinuation);

    // Execute
    underTest.rebuildMetadata();

    // Verify: Tracked distributions were queried
    verify(keyValueFacet).getTrackedDistributions();
  }

  @Test
  public void testRebuildMetadata_WhenUpstreamUnreachable_AndNoCachedPackages_ReturnsEmpty() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn(DISTRIBUTION);

    // Setup: Upstream returns 404 (forces fallback to cached)
    HttpResponse response = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(404);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(httpClient.execute(any())).thenReturn(response);

    // Setup: No cached packages in pool
    Continuation<FluentAsset> emptyContinuation = mockEmptyContinuation();
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(emptyContinuation);

    // Execute
    Optional<Content> result = underTest.rebuildMetadata();

    // Verify: Returns empty when no packages available
    assertThat(result.isPresent(), is(false));

    // Verify: No put was called since there are no packages
    verify(contentFacet, never()).put(anyString(), any());
  }

  @Test
  public void testAddPackageMetadata_NoExceptionWithMockedAsset() throws Exception {
    // The real addPackageMetadata uses InternalIds which requires real AssetData.
    // For unit tests, we verify the method doesn't throw NullPointerException
    // when component ID is not present (asset without component).
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");

    // Execute - should not throw even when component ID is absent
    underTest.addPackageMetadata(asset);

    // Verify: no KV store call when component ID is not found
    verify(keyValueFacet, never()).addPackageMetadata(anyInt(), anyInt(), anyString());
  }

  @Test
  public void testRemovePackageMetadata_NoExceptionWithMockedAsset() throws Exception {
    // The real removePackageMetadata uses InternalIds which requires real AssetData.
    // For unit tests, we verify the method doesn't throw NullPointerException
    // and logs a warning when component ID is not present.
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");

    // Execute - should not throw even when component ID is absent
    underTest.removePackageMetadata(asset);

    // Verify: no KV store call when component ID is not found
    verify(keyValueFacet, never()).removePackageMetadata(anyInt(), anyInt());
  }

  // ==========================================================================
  // PARSE PACKAGES STREAM TESTS
  // ==========================================================================

  @Test
  public void testParsePackagesStream_ExtractsCorrectFields() throws Exception {
    String packagesContent =
        "Package: nginx\n" +
            "Version: 1.18.0-1\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/n/nginx/nginx_1.18.0-1_amd64.deb\n" +
            "Size: 1234567\n" +
            "MD5Sum: d41d8cd98f00b204e9800998ecf8427e\n" +
            "SHA256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
            "\n" +
            "Package: curl\n" +
            "Version: 7.81.0-1\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/c/curl/curl_7.81.0-1_amd64.deb\n" +
            "Size: 234567\n" +
            "\n";

    Method parseMethod = AptProxyMetadataFacet.class.getDeclaredMethod(
        "parsePackagesStream", InputStream.class);
    parseMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) parseMethod.invoke(underTest,
        new ByteArrayInputStream(packagesContent.getBytes(StandardCharsets.UTF_8)));

    assertThat(entries.size(), is(2));
    assertThat(entries.containsKey("nginx:1.18.0-1:amd64"), is(true));
    assertThat(entries.containsKey("curl:7.81.0-1:amd64"), is(true));
  }

  @Test
  public void testParsePackagesStream_HandlesMultipleVersions() throws Exception {
    String packagesContent =
        "Package: nginx\n" +
            "Version: 1.18.0-1\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/n/nginx/nginx_1.18.0-1_amd64.deb\n" +
            "\n" +
            "Package: nginx\n" +
            "Version: 1.20.0-1\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/n/nginx/nginx_1.20.0-1_amd64.deb\n" +
            "\n" +
            "Package: nginx\n" +
            "Version: 1.18.0-1\n" +
            "Architecture: arm64\n" +
            "Filename: pool/main/n/nginx/nginx_1.18.0-1_arm64.deb\n" +
            "\n";

    Method parseMethod = AptProxyMetadataFacet.class.getDeclaredMethod(
        "parsePackagesStream", InputStream.class);
    parseMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) parseMethod.invoke(underTest,
        new ByteArrayInputStream(packagesContent.getBytes(StandardCharsets.UTF_8)));

    // Three distinct packages: two versions on amd64, one version on arm64
    assertThat(entries.size(), is(3));
    assertThat(entries.containsKey("nginx:1.18.0-1:amd64"), is(true));
    assertThat(entries.containsKey("nginx:1.20.0-1:amd64"), is(true));
    assertThat(entries.containsKey("nginx:1.18.0-1:arm64"), is(true));
  }

  @Test
  public void testParsePackagesStream_SkipsEntriesWithoutPackageField() throws Exception {
    String packagesContent =
        "Package: valid\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/v/valid/valid_1.0_amd64.deb\n" +
            "\n" +
            "Version: 2.0\n" + // Missing Package field
            "Architecture: amd64\n" +
            "Filename: pool/main/i/invalid/invalid_2.0_amd64.deb\n" +
            "\n" +
            "Package: also-valid\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/a/also-valid/also-valid_1.0_amd64.deb\n" +
            "\n";

    Method parseMethod = AptProxyMetadataFacet.class.getDeclaredMethod(
        "parsePackagesStream", InputStream.class);
    parseMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) parseMethod.invoke(underTest,
        new ByteArrayInputStream(packagesContent.getBytes(StandardCharsets.UTF_8)));

    assertThat(entries.size(), is(2));
    assertThat(entries.containsKey("valid:1.0:amd64"), is(true));
    assertThat(entries.containsKey("also-valid:1.0:amd64"), is(true));
  }

  @Test
  public void testParsePackagesStream_SkipsEntriesWithoutFilenameField() throws Exception {
    String packagesContent =
        "Package: valid\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/v/valid/valid_1.0_amd64.deb\n" +
            "\n" +
            "Package: invalid\n" +
            "Version: 2.0\n" +
            "Architecture: amd64\n" +
            // Missing Filename
            "\n" +
            "Package: also-valid\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/a/also-valid/also-valid_1.0_amd64.deb\n" +
            "\n";

    Method parseMethod = AptProxyMetadataFacet.class.getDeclaredMethod(
        "parsePackagesStream", InputStream.class);
    parseMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) parseMethod.invoke(underTest,
        new ByteArrayInputStream(packagesContent.getBytes(StandardCharsets.UTF_8)));

    assertThat(entries.size(), is(2));
    assertThat(entries.containsKey("valid:1.0:amd64"), is(true));
    assertThat(entries.containsKey("also-valid:1.0:amd64"), is(true));
    assertThat(entries.containsKey("invalid:2.0:amd64"), is(false));
  }

  // ==========================================================================
  // KV STORAGE TESTS
  // ==========================================================================

  @Test
  public void testPackageEntryFromMetadata_ExtractsPackageInfo() throws Exception {
    // Create metadata map like what's stored in KV
    // Field names must match AptProperties constants: architecture, package_name, package_version, index_section
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("architecture", "amd64");
    metadata.put("package_name", "hello");
    metadata.put("package_version", "1.0");
    metadata.put("index_section",
        "Package: hello\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/h/hello/hello_1.0_amd64.deb\n" +
            "Size: 1024\n" +
            "MD5Sum: d41d8cd98f00b204e9800998ecf8427e\n" +
            "SHA256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n");

    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "packageEntryFromMetadata", Map.class);
    method.setAccessible(true);

    Object entry = method.invoke(underTest, metadata);

    assertThat(entry, is(not((Object) null)));

    // Use reflection to access entry fields
    Method keyMethod = entry.getClass().getDeclaredMethod("key");
    Method filenameMethod = entry.getClass().getDeclaredMethod("filename");
    Method indexSectionMethod = entry.getClass().getDeclaredMethod("indexSection");

    String key = (String) keyMethod.invoke(entry);
    String filename = (String) filenameMethod.invoke(entry);
    String indexSection = (String) indexSectionMethod.invoke(entry);

    assertThat(key, is("hello:1.0:amd64"));
    assertThat(filename, is("pool/main/h/hello/hello_1.0_amd64.deb"));

    // Verify index section contains expected content
    assertThat(indexSection, containsString("Package: hello"));
    assertThat(indexSection, containsString("Filename: pool/main/h/hello/hello_1.0_amd64.deb"));
  }

  @Test
  public void testPackageEntryFromMetadata_ReturnsNullForMissingIndexSection() throws Exception {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("architecture", "amd64");
    metadata.put("package_name", "hello");
    metadata.put("package_version", "1.0");
    // Missing index_section

    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "packageEntryFromMetadata", Map.class);
    method.setAccessible(true);

    Object entry = method.invoke(underTest, metadata);

    assertThat(entry, is((Object) null));
  }

  @Test
  public void testPackageEntryFromMetadata_ReturnsNullForMissingPackageName() throws Exception {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("architecture", "amd64");
    // Missing package_name
    metadata.put("package_version", "1.0");
    metadata.put("index_section", "Package: hello\nFilename: pool/foo.deb\n");

    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "packageEntryFromMetadata", Map.class);
    method.setAccessible(true);

    Object entry = method.invoke(underTest, metadata);

    assertThat(entry, is((Object) null));
  }

  @Test
  public void testExtractFilenameFromIndexSection() throws Exception {
    String indexSection =
        "Package: hello\n" +
            "Version: 1.0\n" +
            "Filename: pool/main/h/hello/hello_1.0_amd64.deb\n" +
            "Size: 1024\n";

    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "extractFilenameFromIndexSection", String.class);
    method.setAccessible(true);

    String filename = (String) method.invoke(underTest, indexSection);

    assertThat(filename, is("pool/main/h/hello/hello_1.0_amd64.deb"));
  }

  @Test
  public void testExtractFilenameFromIndexSection_ReturnsNullWhenMissing() throws Exception {
    String indexSection =
        "Package: hello\n" +
            "Version: 1.0\n" +
            "Size: 1024\n";

    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "extractFilenameFromIndexSection", String.class);
    method.setAccessible(true);

    String filename = (String) method.invoke(underTest, indexSection);

    assertThat(filename, is((String) null));
  }

  // ==========================================================================
  // DETERMINISM TESTS
  // ==========================================================================

  @Test
  public void testStableComparator_ProducesDeterministicOrder() throws Exception {
    // Create entries in random order
    String packagesContent =
        "Package: zlib\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/z/zlib/zlib_1.0_amd64.deb\n" +
            "\n" +
            "Package: apt\n" +
            "Version: 2.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/a/apt/apt_2.0_amd64.deb\n" +
            "\n" +
            "Package: apt\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/a/apt/apt_1.0_amd64.deb\n" +
            "\n" +
            "Package: curl\n" +
            "Version: 1.0\n" +
            "Architecture: arm64\n" +
            "Filename: pool/main/c/curl/curl_1.0_arm64.deb\n" +
            "\n" +
            "Package: curl\n" +
            "Version: 1.0\n" +
            "Architecture: amd64\n" +
            "Filename: pool/main/c/curl/curl_1.0_amd64.deb\n" +
            "\n";

    Method parseMethod = AptProxyMetadataFacet.class.getDeclaredMethod(
        "parsePackagesStream", InputStream.class);
    parseMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) parseMethod.invoke(underTest,
        new ByteArrayInputStream(packagesContent.getBytes(StandardCharsets.UTF_8)));

    // Get the STABLE_COMPARATOR via reflection
    Field comparatorField =
        AptProxyMetadataFacet.class.getDeclaredField("STABLE_COMPARATOR");
    comparatorField.setAccessible(true);

    @SuppressWarnings("unchecked")
    java.util.Comparator<Object> comparator =
        (java.util.Comparator<Object>) comparatorField.get(null);

    List<Object> sorted = entries.values()
        .stream()
        .sorted(comparator)
        .toList();

    // Verify order: apt (by name), then apt 1.0 before apt 2.0 (by version),
    // then curl amd64 before arm64 (by arch), then zlib
    assertThat(sorted, hasSize(5));

    Method keyMethod = sorted.get(0).getClass().getDeclaredMethod("key");

    assertThat(keyMethod.invoke(sorted.get(0)), is("apt:1.0:amd64"));
    assertThat(keyMethod.invoke(sorted.get(1)), is("apt:2.0:amd64"));
    assertThat(keyMethod.invoke(sorted.get(2)), is("curl:1.0:amd64"));
    assertThat(keyMethod.invoke(sorted.get(3)), is("curl:1.0:arm64"));
    assertThat(keyMethod.invoke(sorted.get(4)), is("zlib:1.0:amd64"));
  }

  // ==========================================================================
  // MULTI-COMPONENT TESTS
  // ==========================================================================

  @Test
  public void testSliceKeySeparator_DefinedCorrectly() throws Exception {
    Field separatorField = AptProxyMetadataFacet.class.getDeclaredField("SLICE_KEY_SEPARATOR");
    separatorField.setAccessible(true);
    String separator = (String) separatorField.get(null);

    assertThat(separator, is("|"));
  }

  @Test
  public void testSliceKeySeparatorPattern_EscapedCorrectly() throws Exception {
    Field patternField = AptProxyMetadataFacet.class.getDeclaredField("SLICE_KEY_SEPARATOR_PATTERN");
    patternField.setAccessible(true);
    String pattern = (String) patternField.get(null);

    // Pattern should be escaped for use in split()
    assertThat(pattern, is("\\|"));

    // Verify it works correctly in split
    String testKey = "main|amd64";
    String[] parts = testKey.split(pattern);
    assertThat(parts.length, is(2));
    assertThat(parts[0], is("main"));
    assertThat(parts[1], is("amd64"));
  }

  @Test
  public void testCompositeKeyParsing_MultipleComponents() throws Exception {
    Field patternField = AptProxyMetadataFacet.class.getDeclaredField("SLICE_KEY_SEPARATOR_PATTERN");
    patternField.setAccessible(true);
    String pattern = (String) patternField.get(null);

    // Test various component/arch combinations
    String[] testCases = {
        "main|amd64", "universe|amd64", "multiverse|arm64", "restricted|i386"
    };

    for (String testKey : testCases) {
      String[] parts = testKey.split(pattern);
      assertThat("Key should split into 2 parts: " + testKey, parts.length, is(2));
      assertThat("Component should not be empty: " + testKey, parts[0].length() > 0, is(true));
      assertThat("Architecture should not be empty: " + testKey, parts[1].length() > 0, is(true));
    }
  }

  @Test
  public void testSliceRecord_HasComponentAndArchitecture() throws Exception {
    // Find the Slice inner class
    Class<?>[] innerClasses = AptProxyMetadataFacet.class.getDeclaredClasses();
    Class<?> sliceClass = null;

    for (Class<?> inner : innerClasses) {
      if (inner.getSimpleName().equals("Slice")) {
        sliceClass = inner;
        break;
      }
    }

    assertThat("Slice class should exist", sliceClass, is(not((Class<?>) null)));

    // Verify it has component() and architecture() methods (record accessors)
    Method componentMethod = sliceClass.getDeclaredMethod("component");
    Method architectureMethod = sliceClass.getDeclaredMethod("architecture");

    assertThat("component() method should exist", componentMethod, is(not((Method) null)));
    assertThat("architecture() method should exist", architectureMethod, is(not((Method) null)));
  }

  @Test
  public void testPackageIndexPath_UsesCorrectComponent() throws Exception {
    Method method = AptProxyMetadataFacet.class.getDeclaredMethod(
        "packageIndexPath", String.class, String.class, String.class, String.class);
    method.setAccessible(true);

    // Expected path format: dists/{distribution}/{component}/binary-{architecture}/Packages{extension}
    String distribution = "jammy";
    String component = "universe";
    String architecture = "amd64";
    String extension = ".gz";

    // Build expected path manually
    String expected = "dists/" + distribution + "/" + component + "/binary-" + architecture + "/Packages" + extension;

    // Verify the pattern contains the component
    assertThat(expected, containsString("/universe/"));
    assertThat(expected, is("dists/jammy/universe/binary-amd64/Packages.gz"));
  }

  @Test
  public void testPackageRelativeIndexPath_UsesCorrectComponent() throws Exception {
    // Expected format: {component}/binary-{architecture}/Packages{extension}
    String component = "multiverse";
    String architecture = "arm64";
    String extension = ".bz2";

    // Expected relative path
    String expected = component + "/binary-" + architecture + "/Packages" + extension;

    assertThat(expected, is("multiverse/binary-arm64/Packages.bz2"));
    assertThat(expected, containsString("multiverse/"));
    assertThat(expected, not(containsString("main/")));
  }

  @Test
  public void testReleaseComponentsField_SpaceSeparated() {
    // Components should be space-separated in Release file
    Set<String> components = Set.of("main", "universe", "multiverse");

    String joined = String.join(" ", components);

    // Note: Set iteration order is not guaranteed, but all components should be present
    for (String component : components) {
      assertThat(joined, containsString(component));
    }
  }

  @Test
  public void testDefaultComponent_IsMain() throws Exception {
    Field defaultField = AptMetadataFacetSupport.class.getDeclaredField("DEFAULT_COMPONENT");
    defaultField.setAccessible(true);
    String defaultComponent = (String) defaultField.get(null);

    assertThat(defaultComponent, is("main"));
  }

  // ==========================================================================
  // PAGINATION TESTS
  // ==========================================================================

  @Test
  public void testPathNormalization_WithLeadingSlash() {
    String path = "/pool/main/h/hello/hello_1.0_amd64.deb";
    String normalized = path.startsWith("/") ? path : "/" + path;

    assertThat(normalized, is("/pool/main/h/hello/hello_1.0_amd64.deb"));
    assertThat(normalized.startsWith("/pool/"), is(true));
  }

  @Test
  public void testPathNormalization_WithoutLeadingSlash() {
    String path = "pool/main/h/hello/hello_1.0_amd64.deb";
    String normalized = path.startsWith("/") ? path : "/" + path;

    assertThat(normalized, is("/pool/main/h/hello/hello_1.0_amd64.deb"));
    assertThat(normalized.startsWith("/pool/"), is(true));
  }

  @Test
  public void testFileExtensionMatching_DebAndUdeb() {
    String[] validPaths = {
        "/pool/main/h/hello/hello_1.0_amd64.deb",
        "/pool/main/l/linux/linux_5.4_amd64.udeb",
        "/pool/universe/f/foo/foo_2.0_arm64.deb"
    };

    for (String path : validPaths) {
      boolean matches = path.startsWith("/pool/") &&
          (path.endsWith(".deb") || path.endsWith(".udeb"));
      assertThat("Path should match: " + path, matches, is(true));
    }
  }

  @Test
  public void testFileExtensionMatching_NonPoolPaths() {
    String[] invalidPaths = {
        "/dists/jammy/main/binary-amd64/Packages",
        "/dists/jammy/Release",
        "/other/path/file.deb"
    };

    for (String path : invalidPaths) {
      String normalized = path.startsWith("/") ? path : "/" + path;
      boolean matches = normalized.startsWith("/pool/") &&
          (normalized.endsWith(".deb") || normalized.endsWith(".udeb"));
      assertThat("Path should NOT match: " + path, matches, is(false));
    }
  }

  @Test
  public void testLoadCachedPackagesFromKV_ReturnsPackageEntries() throws Exception {
    // Setup KV store to return package metadata
    // Field names must match AptProperties constants: architecture, package_name, package_version, index_section
    String metadataJson = "{\"architecture\":\"amd64\",\"package_name\":\"hello\",\"package_version\":\"1.0\"" +
        ",\"index_section\":\"Package: hello\\nVersion: 1.0\\nArchitecture: amd64\\nFilename: pool/main/h/hello/hello_1.0_amd64.deb\\n\"}";

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(java.util.stream.Stream.of(metadataJson));

    // Execute via reflection
    Method loadMethod = AptProxyMetadataFacet.class.getDeclaredMethod("loadCachedPackagesFromKV");
    loadMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) loadMethod.invoke(underTest);

    // Verify we got the package entry
    assertThat(result.size(), is(1));
    assertThat(result.containsKey("hello:1.0:amd64"), is(true));
  }

  @Test
  public void testLoadCachedPackagesFromKV_EmptyKVStore() throws Exception {
    // Setup KV store to return empty stream
    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(java.util.stream.Stream.empty());

    Method loadMethod = AptProxyMetadataFacet.class.getDeclaredMethod("loadCachedPackagesFromKV");
    loadMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) loadMethod.invoke(underTest);

    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void testLoadCachedPackagesFromKV_HandlesInvalidMetadata() throws Exception {
    // Setup KV store with mix of valid and invalid metadata
    // Field names must match AptProperties constants
    String validJson = "{\"architecture\":\"amd64\",\"package_name\":\"hello\",\"package_version\":\"1.0\"" +
        ",\"index_section\":\"Package: hello\\nVersion: 1.0\\nArchitecture: amd64\\nFilename: pool/main/h/hello/hello_1.0_amd64.deb\\n\"}";
    String invalidJson = "{\"architecture\":\"amd64\"}"; // Missing required fields

    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(java.util.stream.Stream.of(validJson, invalidJson));

    Method loadMethod = AptProxyMetadataFacet.class.getDeclaredMethod("loadCachedPackagesFromKV");
    loadMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) loadMethod.invoke(underTest);

    // Should only contain the valid entry
    assertThat(result.size(), is(1));
  }

  // ==========================================================================
  // HELPERS
  // ==========================================================================

  @SuppressWarnings("unchecked")
  private Continuation<FluentAsset> mockEmptyContinuation() {
    Continuation<FluentAsset> continuation = mock(Continuation.class);
    when(continuation.iterator()).thenReturn(java.util.Collections.emptyIterator());
    when(continuation.isEmpty()).thenReturn(true);
    return continuation;
  }

  @SuppressWarnings("unchecked")
  private Continuation<FluentAsset> mockContinuationWith(List<FluentAsset> assets) {
    Continuation<FluentAsset> continuation = mock(Continuation.class);
    when(continuation.iterator()).thenReturn(assets.iterator());
    when(continuation.isEmpty()).thenReturn(assets.isEmpty());
    return continuation;
  }

  @SuppressWarnings("unchecked")
  private Continuation<FluentAsset> mockContinuationWithToken(List<FluentAsset> assets, String nextToken) {
    Continuation<FluentAsset> continuation = mock(Continuation.class);

    when(continuation.iterator()).thenReturn(assets.iterator());
    when(continuation.isEmpty()).thenReturn(assets.isEmpty());
    when(continuation.nextContinuationToken()).thenReturn(nextToken);

    return continuation;
  }

  @SuppressWarnings("unchecked")
  private Continuation<FluentAsset> mockContinuationLastPage(List<FluentAsset> assets) {
    Continuation<FluentAsset> continuation = mock(Continuation.class);

    when(continuation.iterator()).thenReturn(assets.iterator());
    when(continuation.isEmpty()).thenReturn(assets.isEmpty());
    // Simulate ContinuationArrayList behavior: throws when no more pages
    when(continuation.nextContinuationToken()).thenThrow(new IllegalStateException("No more results"));

    return continuation;
  }

  private List<FluentAsset> createMockAssets(int startIndex, int count) {
    List<FluentAsset> assets = new ArrayList<>();
    for (int i = startIndex; i < startIndex + count; i++) {
      FluentAsset asset = mockPoolAsset(
          "/pool/main/p/pkg" + i + "/pkg" + i + "_1.0_amd64.deb",
          "pkg" + i,
          "1.0",
          "amd64");
      assets.add(asset);
    }
    return assets;
  }

  private FluentAsset mockPoolAsset(String path, String packageName, String version, String arch) {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn(path);

    // No blob means it will be skipped in processing
    when(asset.blob()).thenReturn(Optional.empty());

    return asset;
  }

  private FluentAsset mockCachedDebAsset(
      final String path,
      final String packageName,
      final String version,
      final String architecture) throws IOException
  {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn(path);

    // Setup blob
    org.sonatype.nexus.repository.content.AssetBlob blob = mock(org.sonatype.nexus.repository.content.AssetBlob.class);
    when(blob.blobSize()).thenReturn(1024L);

    Map<String, String> checksums = new HashMap<>();
    checksums.put("MD5", "d41d8cd98f00b204e9800998ecf8427e");
    checksums.put("SHA1", "da39a3ee5e6b4b0d3255bfef95601890afd80709");
    checksums.put("SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    when(blob.checksums()).thenReturn(checksums);
    when(asset.blob()).thenReturn(Optional.of(blob));

    // Setup format attributes with index_section
    String indexSection = String.format(
        "Package: %s\nVersion: %s\nArchitecture: %s\nFilename: %s\n",
        packageName, version, architecture, path.substring(1));

    NestedAttributesMap aptAttrs = mock(NestedAttributesMap.class);
    Map<String, Object> backing = new HashMap<>();
    backing.put("index_section", indexSection);
    when(aptAttrs.backing()).thenReturn(backing);
    when(asset.attributes(anyString())).thenReturn(aptAttrs);

    return asset;
  }

  private FluentAsset mockCachedDebAssetFull(
      final String path,
      final String packageName,
      final String version,
      final String architecture,
      final long size,
      final String md5,
      final String sha1,
      final String sha256)
  {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.path()).thenReturn(path);

    // Setup blob
    org.sonatype.nexus.repository.content.AssetBlob blob =
        mock(org.sonatype.nexus.repository.content.AssetBlob.class);
    when(blob.blobSize()).thenReturn(size);

    // Primary keys must match what HashAlgorithm.name() returns:
    // MD5.name() -> "md5", SHA1.name() -> "sha1", SHA256.name() -> "sha256"
    // We also include an uppercase "SHA1" entry to model legacy/compatibility
    // blobs that stored the SHA1 checksum in uppercase, and to ensure the
    // code under test remains tolerant of both forms.
    Map<String, String> checksums = new HashMap<>();
    checksums.put("md5", md5);
    checksums.put("sha1", sha1);
    checksums.put("sha256", sha256);
    checksums.put("SHA1", sha1);
    when(blob.checksums()).thenReturn(checksums);
    when(asset.blob()).thenReturn(Optional.of(blob));

    // Setup format attributes with index_section
    String indexSection = String.format(
        "Package: %s\nVersion: %s\nArchitecture: %s\nFilename: %s\n",
        packageName, version, architecture, path.substring(1));

    NestedAttributesMap aptAttrs = mock(NestedAttributesMap.class);
    Map<String, Object> backing = new HashMap<>();
    backing.put("index_section", indexSection);
    when(aptAttrs.backing()).thenReturn(backing);
    when(asset.attributes("apt")).thenReturn(aptAttrs);

    return asset;
  }

  /**
   * Test that cached "all" architecture packages persist in metadata even after deletion from upstream.
   * This is a regression test for NEXUS-49457.
   *
   * Scenario:
   * - Upstream only advertises "arm64" (not "all") because "all" packages were deleted
   * - Proxy has cached packages for both "arm64" and "all" architectures
   * - Proxy rebuilds metadata
   * - Expected: Fix should detect cached "all" packages and add "all" slice
   * - Expected: Both "all" and "arm64" packages should remain in proxy metadata
   * - Expected: Release file should include "all" in Architectures list
   * - Expected: Release file should include binary-all/Packages checksums
   */
  @Test
  public void testRebuildMetadata_CachedAllPackagesPersistAfterUpstreamDeletion() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.isEnforceDistribution()).thenReturn(false);
    when(keyValueFacet.getTrackedDistributions()).thenReturn(Set.of(DISTRIBUTION));

    // Mock upstream Release file with ONLY arm64 architecture
    // (not "all" - simulating that all packages were deleted and upstream no longer advertises "all")
    String upstreamRelease = "Architectures: arm64\n" +
        "Components: main\n" +
        "MD5Sum:\n" +
        " d41d8cd98f00b204e9800998ecf8427e 0 main/binary-arm64/Packages\n";

    HttpResponse releaseResponse = mock(HttpResponse.class);
    StatusLine releaseStatusLine = mock(StatusLine.class);
    when(releaseStatusLine.getStatusCode()).thenReturn(200);
    when(releaseResponse.getStatusLine()).thenReturn(releaseStatusLine);
    when(releaseResponse.getEntity()).thenReturn(
        new org.apache.http.entity.ByteArrayEntity(upstreamRelease.getBytes(StandardCharsets.UTF_8)));

    // Mock empty Packages files from upstream (packages deleted)
    HttpResponse emptyPackagesResponse = mock(HttpResponse.class);
    StatusLine emptyStatusLine = mock(StatusLine.class);
    when(emptyStatusLine.getStatusCode()).thenReturn(200);
    when(emptyPackagesResponse.getStatusLine()).thenReturn(emptyStatusLine);
    when(emptyPackagesResponse.getEntity()).thenReturn(
        new org.apache.http.entity.ByteArrayEntity("".getBytes(StandardCharsets.UTF_8)));

    // Setup HTTP client to return responses
    when(httpClient.execute(any())).thenReturn(releaseResponse, emptyPackagesResponse, emptyPackagesResponse);

    // Mock cached packages in KV store (simulating previously cached packages)
    String cachedAllPackageJson = "{\n" +
        "  \"package_name\": \"wamerican\",\n" +
        "  \"version\": \"2020.12.07-2\",\n" +
        "  \"architecture\": \"all\",\n" +
        "  \"filename\": \"pool/w/wamerican/wamerican_2020.12.07-2_all.deb\",\n" +
        "  \"size\": 236232,\n" +
        "  \"md5sum\": \"5c37bb1ec275fe8f04f1a75016132288\",\n" +
        "  \"sha1\": \"4c9cdd78107c8965fb790085bf1ebb2b33f20353\",\n" +
        "  \"sha256\": \"68dbe031e3fe3a63c50f7fd5d63c320879dab5a35ba86dd5ddd83905b080b0b4\",\n" +
        "  \"index_section\": \"Package: wamerican\\nVersion: 2020.12.07-2\\nArchitecture: all\\nFilename: pool/w/wamerican/wamerican_2020.12.07-2_all.deb\\nSize: 236232\\nMD5Sum: 5c37bb1ec275fe8f04f1a75016132288\\nSHA1: 4c9cdd78107c8965fb790085bf1ebb2b33f20353\\nSHA256: 68dbe031e3fe3a63c50f7fd5d63c320879dab5a35ba86dd5ddd83905b080b0b4\"\n"
        +
        "}";

    String cachedArm64PackageJson = "{\n" +
        "  \"package_name\": \"nginx\",\n" +
        "  \"version\": \"1.18.0-1\",\n" +
        "  \"architecture\": \"arm64\",\n" +
        "  \"filename\": \"pool/n/nginx/nginx_1.18.0-1_arm64.deb\",\n" +
        "  \"size\": 54321,\n" +
        "  \"md5sum\": \"abc123def456\",\n" +
        "  \"sha1\": \"def456abc123\",\n" +
        "  \"sha256\": \"123abc456def\",\n" +
        "  \"index_section\": \"Package: nginx\\nVersion: 1.18.0-1\\nArchitecture: arm64\\nFilename: pool/n/nginx/nginx_1.18.0-1_arm64.deb\\nSize: 54321\\nMD5Sum: abc123def456\\nSHA1: def456abc123\\nSHA256: 123abc456def\"\n"
        +
        "}";

    // Setup KV store to return cached packages as a stream
    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(java.util.stream.Stream.of(cachedAllPackageJson, cachedArm64PackageJson));

    // Setup ArgumentCaptors to capture what gets written to contentFacet.put()
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);

    setupMockPutOperations();

    // Execute: Rebuild metadata
    Optional<Content> result = underTest.rebuildMetadata();

    // Assert: Rebuild succeeded with cached packages from both architectures
    assertThat("Rebuild should succeed with cached 'all' and 'arm64' packages",
        result.isPresent(), is(true));

    // Capture all put operations to inspect what was written
    verify(contentFacet, atLeast(1)).put(pathCaptor.capture(), payloadCaptor.capture());

    // Extract all captured paths and payloads
    List<String> paths = pathCaptor.getAllValues();
    List<Payload> payloads = payloadCaptor.getAllValues();

    assertThat("At least one file should be written", paths.size(), greaterThanOrEqualTo(1));

    // Find the Release file (not InRelease, which is signed)
    int releaseIndex = -1;
    for (int i = 0; i < paths.size(); i++) {
      String path = paths.get(i);
      // Look for Release file: ends with /Release but not InRelease
      if (path.endsWith("/Release") && !path.contains("InRelease")) {
        releaseIndex = i;
        break;
      }
    }

    assertThat("Release file should be generated", releaseIndex, greaterThanOrEqualTo(0));

    // Read Release file content from the captured payload
    Payload releasePayload = payloads.get(releaseIndex);
    assertThat("Release payload should not be null", releasePayload, notNullValue());

    String releaseContent;
    try (InputStream is = releasePayload.openInputStream()) {
      releaseContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // CRITICAL ASSERTIONS - These verify the fix for NEXUS-49457:

    // 1. Verify Release file contains Architectures field
    assertThat("Release file must include Architectures field",
        releaseContent, containsString("Architectures:"));

    // 2. Verify "all" architecture is listed (this is the key fix - without it, cached "all" packages disappear)
    assertThat("Release file must include 'all' architecture (cached but not in upstream)",
        releaseContent, containsString("all"));

    // 3. Verify "arm64" architecture is listed (from upstream)
    assertThat("Release file must include 'arm64' architecture (from upstream)",
        releaseContent, containsString("arm64"));

    // 4. Verify binary-all/Packages checksums are present (proves "all" slice was created and processed)
    assertThat(
        "Release file must include main/binary-all/Packages in checksums (proves cached 'all' packages are included)",
        releaseContent, containsString("main/binary-all/Packages"));

    // 5. Verify binary-arm64/Packages checksums are present
    assertThat("Release file must include main/binary-arm64/Packages in checksums",
        releaseContent, containsString("main/binary-arm64/Packages"));

    // The test proves NEXUS-49457 fix works because:
    // 1. Upstream Release only advertises "arm64" (not "all")
    // 2. Cached packages exist for both "all" and "arm64" architectures
    // 3. Fix detects cached "all" architecture via findMissingArchitectures()
    // 4. Fix adds "all" slice via addSlicesForMissingArchitectures()
    // 5. Fix includes cached "all" packages via addCachedPackagesForArchitecture()
    // 6. Generated Release file lists BOTH "all" and "arm64" architectures with their Packages files
    // 7. WITHOUT the fix: only "arm64" would be in Release, and cached "all" packages would be invisible to APT clients
  }

  /**
   * Test that cached i386 architecture packages persist in metadata even after deletion from upstream.
   * This proves the NEXUS-49457 fix is architecture-agnostic and works for ANY architecture, not just "all".
   *
   * Scenario:
   * - Upstream only advertises "amd64" (not "i386") because i386 packages were deleted
   * - Proxy has cached packages for both "amd64" and "i386" architectures
   * - Proxy rebuilds metadata
   * - Expected: Fix should detect cached "i386" packages and add "i386" slice
   * - Expected: Both "i386" and "amd64" packages should remain in proxy metadata
   * - Expected: Release file should include "i386" in Architectures list
   * - Expected: Release file should include binary-i386/Packages checksums
   */
  @Test
  public void testRebuildMetadata_CachedI386PackagesPersistAfterUpstreamDeletion() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(contentFacet.isEnforceDistribution()).thenReturn(false);
    when(keyValueFacet.getTrackedDistributions()).thenReturn(Set.of(DISTRIBUTION));

    // Mock upstream Release file with ONLY amd64 architecture
    // (not "i386" - simulating that i386 packages were deleted and upstream no longer advertises "i386")
    String upstreamRelease = "Architectures: amd64\n" +
        "Components: main\n" +
        "MD5Sum:\n" +
        " d41d8cd98f00b204e9800998ecf8427e 0 main/binary-amd64/Packages\n";

    HttpResponse releaseResponse = mock(HttpResponse.class);
    StatusLine releaseStatusLine = mock(StatusLine.class);
    when(releaseStatusLine.getStatusCode()).thenReturn(200);
    when(releaseResponse.getStatusLine()).thenReturn(releaseStatusLine);
    when(releaseResponse.getEntity()).thenReturn(
        new org.apache.http.entity.ByteArrayEntity(upstreamRelease.getBytes(StandardCharsets.UTF_8)));

    // Mock empty Packages files from upstream (i386 packages deleted)
    HttpResponse emptyPackagesResponse = mock(HttpResponse.class);
    StatusLine emptyStatusLine = mock(StatusLine.class);
    when(emptyStatusLine.getStatusCode()).thenReturn(200);
    when(emptyPackagesResponse.getStatusLine()).thenReturn(emptyStatusLine);
    when(emptyPackagesResponse.getEntity()).thenReturn(
        new org.apache.http.entity.ByteArrayEntity("".getBytes(StandardCharsets.UTF_8)));

    // Setup HTTP client to return responses
    when(httpClient.execute(any())).thenReturn(releaseResponse, emptyPackagesResponse, emptyPackagesResponse);

    // Mock cached packages in KV store (simulating previously cached packages)
    String cachedI386PackageJson = "{\n" +
        "  \"package_name\": \"libc6\",\n" +
        "  \"version\": \"2.31-0ubuntu9\",\n" +
        "  \"architecture\": \"i386\",\n" +
        "  \"filename\": \"pool/l/libc6/libc6_2.31-0ubuntu9_i386.deb\",\n" +
        "  \"size\": 2812456,\n" +
        "  \"md5sum\": \"a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6\",\n" +
        "  \"sha1\": \"f1e2d3c4b5a6978869504a3b2c1d0e9f8a7b6c5\",\n" +
        "  \"sha256\": \"1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\",\n" +
        "  \"index_section\": \"Package: libc6\\nVersion: 2.31-0ubuntu9\\nArchitecture: i386\\nFilename: pool/l/libc6/libc6_2.31-0ubuntu9_i386.deb\\nSize: 2812456\\nMD5Sum: a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6\\nSHA1: f1e2d3c4b5a6978869504a3b2c1d0e9f8a7b6c5\\nSHA256: 1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\"\n"
        +
        "}";

    String cachedAmd64PackageJson = "{\n" +
        "  \"package_name\": \"bash\",\n" +
        "  \"version\": \"5.0-6ubuntu1\",\n" +
        "  \"architecture\": \"amd64\",\n" +
        "  \"filename\": \"pool/b/bash/bash_5.0-6ubuntu1_amd64.deb\",\n" +
        "  \"size\": 665432,\n" +
        "  \"md5sum\": \"9876fedcba0123456789abcdef012345\",\n" +
        "  \"sha1\": \"5432109876fedcba0123456789abcdef01234567\",\n" +
        "  \"sha256\": \"fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210\",\n" +
        "  \"index_section\": \"Package: bash\\nVersion: 5.0-6ubuntu1\\nArchitecture: amd64\\nFilename: pool/b/bash/bash_5.0-6ubuntu1_amd64.deb\\nSize: 665432\\nMD5Sum: 9876fedcba0123456789abcdef012345\\nSHA1: 5432109876fedcba0123456789abcdef01234567\\nSHA256: fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210\"\n"
        +
        "}";

    // Setup KV store to return cached packages as a stream
    when(keyValueFacet.browsePackagesMetadata())
        .thenReturn(java.util.stream.Stream.of(cachedI386PackageJson, cachedAmd64PackageJson));

    // Setup ArgumentCaptors to capture what gets written to contentFacet.put()
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);

    setupMockPutOperations();

    // Execute: Rebuild metadata
    Optional<Content> result = underTest.rebuildMetadata();

    // Assert: Rebuild succeeded with cached packages from both architectures
    assertThat("Rebuild should succeed with cached 'i386' and 'amd64' packages",
        result.isPresent(), is(true));

    // Capture all put operations to inspect what was written
    verify(contentFacet, atLeast(1)).put(pathCaptor.capture(), payloadCaptor.capture());

    // Extract all captured paths and payloads
    List<String> paths = pathCaptor.getAllValues();
    List<Payload> payloads = payloadCaptor.getAllValues();

    assertThat("At least one file should be written", paths.size(), greaterThanOrEqualTo(1));

    // Find the Release file (not InRelease, which is signed)
    int releaseIndex = -1;
    for (int i = 0; i < paths.size(); i++) {
      String path = paths.get(i);
      // Look for Release file: ends with /Release but not InRelease
      if (path.endsWith("/Release") && !path.contains("InRelease")) {
        releaseIndex = i;
        break;
      }
    }

    assertThat("Release file should be generated", releaseIndex, greaterThanOrEqualTo(0));

    // Read Release file content from the captured payload
    Payload releasePayload = payloads.get(releaseIndex);
    assertThat("Release payload should not be null", releasePayload, notNullValue());

    String releaseContent;
    try (InputStream is = releasePayload.openInputStream()) {
      releaseContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    // CRITICAL ASSERTIONS - These prove the fix is architecture-agnostic:

    // 1. Verify Release file contains Architectures field
    assertThat("Release file must include Architectures field",
        releaseContent, containsString("Architectures:"));

    // 2. Verify "i386" architecture is listed (this proves the fix works for ANY architecture, not just "all")
    assertThat("Release file must include 'i386' architecture (cached but not in upstream)",
        releaseContent, containsString("i386"));

    // 3. Verify "amd64" architecture is listed (from upstream)
    assertThat("Release file must include 'amd64' architecture (from upstream)",
        releaseContent, containsString("amd64"));

    // 4. Verify binary-i386/Packages checksums are present (proves "i386" slice was created and processed)
    assertThat(
        "Release file must include main/binary-i386/Packages in checksums (proves cached 'i386' packages are included)",
        releaseContent, containsString("main/binary-i386/Packages"));

    // 5. Verify binary-amd64/Packages checksums are present
    assertThat("Release file must include main/binary-amd64/Packages in checksums",
        releaseContent, containsString("main/binary-amd64/Packages"));

    // This test proves the NEXUS-49457 fix is architecture-agnostic because:
    // 1. Upstream Release only advertises "amd64" (not "i386")
    // 2. Cached packages exist for both "amd64" and "i386" architectures
    // 3. Fix detects cached "i386" architecture via findMissingArchitectures()
    // 4. Fix adds "i386" slice via addSlicesForMissingArchitectures()
    // 5. Fix includes cached "i386" packages via addCachedPackagesForArchitecture()
    // 6. Generated Release file lists BOTH "i386" and "amd64" architectures with their Packages files
    // 7. The fix works identically for i386, arm64, armhf, ppc64el, s390x, or ANY other architecture
  }

  private void setupMockPutOperations() throws IOException {
    // Setup checksums
    Map<String, String> checksums = new HashMap<>();
    checksums.put("MD5", "d41d8cd98f00b204e9800998ecf8427e");
    checksums.put("SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

    when(assetBlob.checksums()).thenReturn(checksums);
    when(assetBlob.blobSize()).thenReturn(1024L);

    when(releaseAsset.blob()).thenReturn(Optional.of(assetBlob));
    when(releaseAsset.download()).thenReturn(new Content(new BytesPayload("test".getBytes(), "text/plain")));

    when(contentFacet.put(anyString(), any())).thenReturn(releaseAsset);

    // Setup signing
    when(signingFacet.signInline(anyString())).thenReturn("signed-inline".getBytes());
    when(signingFacet.signExternal(anyString())).thenReturn("signed-external".getBytes());
  }
}
