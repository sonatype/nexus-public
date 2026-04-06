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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AptProxyMetadataHandler}.
 */
public class AptProxyMetadataHandlerTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private Repository repository;

  @Mock
  private Request request;

  @Mock
  private AptContentFacet contentFacet;

  @Mock
  private AptKeyValueFacet keyValueFacet;

  @Mock
  private AptSigningFacet signingFacet;

  @Mock
  private AptProxyMetadataFacet metadataFacet;

  @Mock
  private Response proceedResponse;

  private AptProxyMetadataHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new AptProxyMetadataHandler();

    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(context.proceed()).thenReturn(proceedResponse);
    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(AptKeyValueFacet.class)).thenReturn(keyValueFacet);
    when(repository.facet(AptSigningFacet.class)).thenReturn(signingFacet);
    when(repository.facet(AptProxyMetadataFacet.class)).thenReturn(metadataFacet);

    // Default behavior: metadataFacet.getMetadata() delegates to contentFacet.get()
    // Tests can override this behavior as needed
    when(metadataFacet.getMetadata(any(Context.class), anyString(), anyString())).thenAnswer(invocation -> {
      String path = invocation.getArgument(1);
      return contentFacet.get(path);
    });
  }

  /**
   * Helper method to create Content with proper CacheInfo attributes.
   * This ensures the staleness check doesn't fail due to missing CacheInfo.
   */
  private Content createContentWithCacheInfo(byte[] data, String contentType) {
    Content content = new Content(new BytesPayload(data, contentType));
    AttributesMap attributes = content.getAttributes();
    CacheInfo cacheInfo = mock(CacheInfo.class);
    attributes.set(CacheInfo.class, cacheInfo);
    return content;
  }

  @Test
  public void testHandle_WhenSigningNotConfigured_ShouldProceed() throws Exception {
    // Setup: Signing not configured
    when(signingFacet.isConfigured()).thenReturn(false);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough to upstream
    assertThat(response, is(proceedResponse));
    verify(context).proceed();

    // Verify: Distribution is tracked even when signing not configured
    verify(keyValueFacet).trackDistribution("jammy");
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndNonDistsPath_ShouldProceed() throws Exception {
    // Setup: Signing configured, non-dists path (pool file)
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough for non-dists paths
    assertThat(response, is(proceedResponse));
    verify(context).proceed();

    // Verify: No distribution tracking for non-dists paths
    verify(keyValueFacet, never()).trackDistribution(anyString());
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndDistsPath_ShouldTrackDistribution() throws Exception {
    // Setup: Signing configured, dists path
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // No generated metadata available
    when(contentFacet.get(anyString())).thenReturn(Optional.empty());

    // Execute
    underTest.handle(context);

    // Verify: Distribution was tracked
    verify(keyValueFacet).trackDistribution("jammy");
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndMetadataGenerated_ShouldServeGenerated() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // Generated metadata exists
    Content generatedContent = createContentWithCacheInfo("generated-inrelease".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/InRelease")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content (not proceed)
    assertThat(response.getStatus().getCode(), is(200));

    // Verify: Did NOT proceed to upstream
    verify(context, never()).proceed();
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndNoMetadata_AndNoGeneratedRelease_ShouldProceed() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // No generated metadata
    when(contentFacet.get(anyString())).thenReturn(Optional.empty());

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough to upstream (no generated metadata yet)
    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndMetadataExists_ButRequestedFileMissing_ShouldReturn503() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages.gz");

    // No generated Packages.gz
    when(contentFacet.get("/dists/jammy/main/binary-amd64/Packages.gz")).thenReturn(Optional.empty());

    // But Release exists (meaning we have generated metadata for this distribution)
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(releaseContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Returns 503 (metadata rebuild in progress)
    assertThat(response.getStatus().getCode(), is(503));

    // Verify: Did NOT proceed to upstream (would cause hash mismatch)
    verify(context, never()).proceed();
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndReleaseFile_ShouldServeGenerated() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/Release");

    // Generated Release exists
    Content generatedContent = createContentWithCacheInfo("release-content".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
  }

  @Test
  public void testHandle_WhenSigningConfigured_AndReleaseGpg_ShouldServeGenerated() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/focal/Release.gpg");

    // Generated Release.gpg exists
    Content generatedContent = createContentWithCacheInfo("signature".getBytes(), "application/pgp-signature");
    when(contentFacet.get("/dists/focal/Release.gpg")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();

    // Verify: Distribution was tracked
    verify(keyValueFacet).trackDistribution("focal");
  }

  @Test
  public void testHandle_WithPathWithoutLeadingSlash_ShouldStillMatch() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("dists/bionic/Release");

    // Generated Release exists
    Content generatedContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("dists/bionic/Release")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Still matched and served
    assertThat(response.getStatus().getCode(), is(200));

    // Verify: Distribution was tracked
    verify(keyValueFacet).trackDistribution("bionic");
  }

  @Test
  public void testHandle_WithNonMetadataDistsPath_ShouldProceed() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/someotherfile.txt");

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough (not a metadata file pattern)
    assertThat(response, is(proceedResponse));
    verify(context).proceed();

    // Verify: Distribution was still tracked (it's under dists/)
    verify(keyValueFacet).trackDistribution("jammy");
  }

  @Test
  public void testHandle_WhenPackagesFileRequested_AndGenerated_ShouldServe() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages");

    // Generated Packages exists
    Content generatedContent = createContentWithCacheInfo("packages".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/main/binary-amd64/Packages")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
  }

  @Test
  public void testHandle_WhenPackagesGzRequested_AndGenerated_ShouldServe() throws Exception {
    // Setup: Signing configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/focal/universe/binary-arm64/Packages.gz");

    // Generated Packages.gz exists
    Content generatedContent = createContentWithCacheInfo("packages-gz".getBytes(), "application/gzip");
    when(contentFacet.get("/dists/focal/universe/binary-arm64/Packages.gz")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));

    // Verify: Distribution was tracked
    verify(keyValueFacet).trackDistribution("focal");
  }

  // =========================================================================
  // NO-MIXING / MISMATCH PREVENTION TESTS
  // These tests verify that when in "generated mode", we NEVER fall back to
  // upstream, which would cause apt hash/size mismatch failures.
  // =========================================================================

  /**
   * GIVEN: Distribution "jammy" is in generated mode (Release exists)
   * AND: InRelease does NOT exist locally (race condition during rebuild)
   * WHEN: Client requests /dists/jammy/InRelease
   * THEN: Handler returns 503 (Service Unavailable)
   * AND: Handler does NOT call context.proceed() (no upstream fallback)
   */
  @Test
  public void testNoMixing_InGeneratedMode_MissingInRelease_Returns503_NeverProceeds() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // InRelease missing
    when(contentFacet.get("/dists/jammy/InRelease")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(releaseContent));

    Response response = underTest.handle(context);

    // MUST return 503, NOT proceed to upstream
    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
  }

  /**
   * GIVEN: Distribution "jammy" is in generated mode (Release exists)
   * AND: Release.gpg does NOT exist locally
   * WHEN: Client requests /dists/jammy/Release.gpg
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testNoMixing_InGeneratedMode_MissingReleaseGpg_Returns503_NeverProceeds() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/Release.gpg");

    // Release.gpg missing
    when(contentFacet.get("/dists/jammy/Release.gpg")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(releaseContent));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
  }

  /**
   * GIVEN: Distribution "jammy" is in generated mode (Release exists)
   * AND: Packages (plain) does NOT exist locally
   * WHEN: Client requests /dists/jammy/main/binary-amd64/Packages
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testNoMixing_InGeneratedMode_MissingPackagesPlain_Returns503_NeverProceeds() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages");

    // Packages missing
    when(contentFacet.get("/dists/jammy/main/binary-amd64/Packages")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(releaseContent));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
  }

  /**
   * GIVEN: Distribution "jammy" is in generated mode (Release exists)
   * AND: Packages.bz2 does NOT exist locally
   * WHEN: Client requests /dists/jammy/main/binary-amd64/Packages.bz2
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testNoMixing_InGeneratedMode_MissingPackagesBz2_Returns503_NeverProceeds() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages.bz2");

    // Packages.bz2 missing
    when(contentFacet.get("/dists/jammy/main/binary-amd64/Packages.bz2")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.of(releaseContent));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
  }

  /**
   * GIVEN: Distribution "jammy" is NOT in generated mode (no Release exists)
   * AND: No Packages.gz exists locally
   * WHEN: Client requests /dists/jammy/main/binary-amd64/Packages.gz
   * THEN: Handler calls context.proceed() (safe passthrough to upstream)
   *
   * This is safe because we haven't generated any metadata yet, so there's
   * no risk of mixing upstream Release with generated Packages.
   */
  @Test
  public void testNoMixing_NotInGeneratedMode_MissingMetadata_SafelyProceedsToUpstream() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages.gz");

    // Neither the requested file nor Release exist = NOT in generated mode
    when(contentFacet.get(anyString())).thenReturn(Optional.empty());

    Response response = underTest.handle(context);

    // Safe to proceed because we're not in generated mode
    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  /**
   * GIVEN: Distribution "focal" is in generated mode
   * AND: Distribution "jammy" is NOT in generated mode
   * AND: Client requests metadata for "jammy"
   * WHEN: Handler processes the request
   * THEN: Handler proceeds to upstream for "jammy" (independent distributions)
   */
  @Test
  public void testNoMixing_DifferentDistributions_IndependentGeneratedMode() throws Exception {
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/dists/jammy/InRelease");

    // jammy has no generated Release
    when(contentFacet.get("/dists/jammy/InRelease")).thenReturn(Optional.empty());
    when(contentFacet.get("/dists/jammy/Release")).thenReturn(Optional.empty());

    // Note: focal could have generated metadata, but it shouldn't affect jammy

    Response response = underTest.handle(context);

    // jammy is not in generated mode, so safe to proceed
    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  /**
   * GIVEN: Flat repository with signing NOT configured
   * WHEN: Client requests root-level Release file
   * THEN: Handler passes through to upstream
   * AND: Distribution is tracked using configured distribution
   */
  @Test
  public void testFlatRepo_SigningNotConfigured_ShouldProceed() throws Exception {
    // Setup: Flat repository, signing not configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(false);
    when(request.getPath()).thenReturn("/Release");

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough to upstream
    assertThat(response, is(proceedResponse));
    verify(context).proceed();

    // Verify: Distribution is tracked even when signing not configured
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * WHEN: Client requests pool file (package)
   * THEN: Handler passes through (pool files are not metadata)
   * AND: No distribution tracking occurs
   */
  @Test
  public void testFlatRepo_PoolFile_ShouldProceed() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough for pool files
    assertThat(response, is(proceedResponse));
    verify(context).proceed();

    // Verify: No distribution tracking for non-metadata paths
    verify(keyValueFacet, never()).trackDistribution(anyString());
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Release file exists
   * WHEN: Client requests /Release
   * THEN: Handler serves the generated Release file
   * AND: Does NOT proceed to upstream
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedRelease() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Release");

    // Generated Release exists
    Content generatedContent = createContentWithCacheInfo("generated-release".getBytes(), "text/plain");
    when(contentFacet.get("/Release")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));

    // Verify: Did NOT proceed to upstream
    verify(context, never()).proceed();

    // Verify: Distribution was tracked
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated InRelease file exists
   * WHEN: Client requests /InRelease
   * THEN: Handler serves the generated InRelease file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedInRelease() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/InRelease");

    // Generated InRelease exists
    Content generatedContent = createContentWithCacheInfo("generated-inrelease".getBytes(), "text/plain");
    when(contentFacet.get("/InRelease")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Release.gpg file exists
   * WHEN: Client requests /Release.gpg
   * THEN: Handler serves the generated signature file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedReleaseGpg() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Release.gpg");

    // Generated Release.gpg exists
    Content generatedContent = createContentWithCacheInfo("signature".getBytes(), "application/pgp-signature");
    when(contentFacet.get("/Release.gpg")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Packages file exists
   * WHEN: Client requests /Packages
   * THEN: Handler serves the generated Packages file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedPackages() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages");

    // Generated Packages exists
    Content generatedContent = createContentWithCacheInfo("packages".getBytes(), "text/plain");
    when(contentFacet.get("/Packages")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Packages.gz file exists
   * WHEN: Client requests /Packages.gz
   * THEN: Handler serves the generated compressed Packages file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedPackagesGz() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages.gz");

    // Generated Packages.gz exists
    Content generatedContent = createContentWithCacheInfo("packages-gz".getBytes(), "application/gzip");
    when(contentFacet.get("/Packages.gz")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Packages.bz2 file exists
   * WHEN: Client requests /Packages.bz2
   * THEN: Handler serves the generated bzip2-compressed Packages file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedPackagesBz2() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages.bz2");

    // Generated Packages.bz2 exists
    Content generatedContent = createContentWithCacheInfo("packages-bz2".getBytes(), "application/x-bzip2");
    when(contentFacet.get("/Packages.bz2")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: Generated Packages.xz file exists
   * WHEN: Client requests /Packages.xz
   * THEN: Handler serves the generated xz-compressed Packages file
   */
  @Test
  public void testFlatRepo_SigningConfigured_ServeGeneratedPackagesXz() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages.xz");

    // Generated Packages.xz exists
    Content generatedContent = createContentWithCacheInfo("packages-xz".getBytes(), "application/x-xz");
    when(contentFacet.get("/Packages.xz")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Served generated content
    assertThat(response.getStatus().getCode(), is(200));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with signing configured
   * AND: No generated metadata exists yet
   * WHEN: Client requests /InRelease
   * THEN: Handler proceeds to upstream (initial state before rebuild)
   */
  @Test
  public void testFlatRepo_NoMetadataYet_ShouldProceed() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/InRelease");

    // No generated metadata exists
    when(contentFacet.get(anyString())).thenReturn(Optional.empty());

    // Execute
    Response response = underTest.handle(context);

    // Verify: Passthrough to upstream (no generated metadata yet)
    assertThat(response, is(proceedResponse));
    verify(context).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  // =========================================================================
  // FLAT REPOSITORY NO-MIXING TESTS
  // Verify that flat repositories prevent mixing generated and upstream metadata
  // =========================================================================

  /**
   * GIVEN: Flat repository is in generated mode (Release exists)
   * AND: InRelease does NOT exist locally (race condition during rebuild)
   * WHEN: Client requests /InRelease
   * THEN: Handler returns 503 (Service Unavailable)
   * AND: Handler does NOT call context.proceed() (no upstream fallback)
   */
  @Test
  public void testFlatRepo_InGeneratedMode_MissingInRelease_Returns503() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/InRelease");

    // InRelease missing
    when(contentFacet.get("/InRelease")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/Release")).thenReturn(Optional.of(releaseContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: MUST return 503, NOT proceed to upstream
    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository is in generated mode (Release exists)
   * AND: Release.gpg does NOT exist locally
   * WHEN: Client requests /Release.gpg
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testFlatRepo_InGeneratedMode_MissingReleaseGpg_Returns503() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Release.gpg");

    // Release.gpg missing
    when(contentFacet.get("/Release.gpg")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/Release")).thenReturn(Optional.of(releaseContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Returns 503
    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository is in generated mode (Release exists)
   * AND: Packages does NOT exist locally
   * WHEN: Client requests /Packages
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testFlatRepo_InGeneratedMode_MissingPackages_Returns503() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages");

    // Packages missing
    when(contentFacet.get("/Packages")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/Release")).thenReturn(Optional.of(releaseContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Returns 503
    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository is in generated mode (Release exists)
   * AND: Packages.gz does NOT exist locally
   * WHEN: Client requests /Packages.gz
   * THEN: Handler returns 503
   * AND: Handler does NOT call context.proceed()
   */
  @Test
  public void testFlatRepo_InGeneratedMode_MissingPackagesGz_Returns503() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages.gz");

    // Packages.gz missing
    when(contentFacet.get("/Packages.gz")).thenReturn(Optional.empty());

    // But Release exists = we are in generated mode
    Content releaseContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("/Release")).thenReturn(Optional.of(releaseContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Returns 503
    assertThat(response.getStatus().getCode(), is(503));
    verify(context, never()).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository is NOT in generated mode (no Release exists)
   * AND: No Packages.gz exists locally
   * WHEN: Client requests /Packages.gz
   * THEN: Handler calls context.proceed() (safe passthrough to upstream)
   *
   * This is safe because we haven't generated any metadata yet, so there's
   * no risk of mixing upstream Release with generated Packages.
   */
  @Test
  public void testFlatRepo_NotInGeneratedMode_MissingMetadata_SafelyProceeds() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("/Packages.gz");

    // Neither the requested file nor Release exist = NOT in generated mode
    when(contentFacet.get(anyString())).thenReturn(Optional.empty());

    // Execute
    Response response = underTest.handle(context);

    // Verify: Safe to proceed because we're not in generated mode
    assertThat(response, is(proceedResponse));
    verify(context).proceed();
    verify(keyValueFacet).trackDistribution("stable");
  }

  /**
   * GIVEN: Flat repository with path without leading slash
   * WHEN: Client requests Release file
   * THEN: Handler should still match and process correctly
   */
  @Test
  public void testFlatRepo_PathWithoutLeadingSlash_ShouldMatch() throws Exception {
    // Setup: Flat repository, signing configured
    when(contentFacet.isFlat()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("stable");
    when(signingFacet.isConfigured()).thenReturn(true);
    when(request.getPath()).thenReturn("Release");

    // Generated Release exists
    Content generatedContent = createContentWithCacheInfo("release".getBytes(), "text/plain");
    when(contentFacet.get("Release")).thenReturn(Optional.of(generatedContent));

    // Execute
    Response response = underTest.handle(context);

    // Verify: Still matched and served
    assertThat(response.getStatus().getCode(), is(200));
    verify(keyValueFacet).trackDistribution("stable");
  }
}
