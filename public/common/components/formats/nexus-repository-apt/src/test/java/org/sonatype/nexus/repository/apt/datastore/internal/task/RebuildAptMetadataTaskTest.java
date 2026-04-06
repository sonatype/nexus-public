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
package org.sonatype.nexus.repository.apt.datastore.internal.task;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RebuildAptMetadataTaskTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private GroupType groupType;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private Repository aptRepository;

  @Mock
  private AptContentFacet hostedContentFacet;

  @Mock
  private AptContentFacet proxyContentFacet;

  @Mock
  private AptKeyValueFacet hostedKeyValueFacet;

  @Mock
  private AptKeyValueFacet proxyKeyValueFacet;

  @Mock
  private AptHostedMetadataFacet metadataFacet;

  @Mock
  private AptProxyMetadataFacet proxyMetadataFacet;

  @Mock
  private AptSigningFacet signingFacet;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private Repository aptProxyRepository;

  private RebuildAptMetadataTask underTest;

  @Before
  public void setup() {
    ThreadContext.bind(securityManager);

    underTest = new RebuildAptMetadataTask();
    underTest.install(repositoryManager, groupType);

    // Setup hosted repository mocks
    when(aptRepository.getName()).thenReturn("test-apt-repo");
    when(aptRepository.getFormat()).thenReturn(new AptFormat());
    when(aptRepository.getType()).thenReturn(new HostedType());
    when(aptRepository.facet(AptContentFacet.class)).thenReturn(hostedContentFacet);
    when(aptRepository.facet(AptKeyValueFacet.class)).thenReturn(hostedKeyValueFacet);
    when(aptRepository.facet(AptHostedMetadataFacet.class)).thenReturn(metadataFacet);

    // Mock getAptPackageAssets to return empty iterable
    when(hostedContentFacet.getAptPackageAssets()).thenReturn(Collections.emptyList());

    when(repositoryManager.get("test-apt-repo")).thenReturn(aptRepository);

    // Setup proxy repository mocks (separate facets from hosted)
    when(aptProxyRepository.getName()).thenReturn("test-apt-proxy");
    when(aptProxyRepository.getFormat()).thenReturn(new AptFormat());
    when(aptProxyRepository.getType()).thenReturn(new ProxyType());
    when(aptProxyRepository.facet(AptContentFacet.class)).thenReturn(proxyContentFacet);
    when(aptProxyRepository.facet(AptKeyValueFacet.class)).thenReturn(proxyKeyValueFacet);
    when(aptProxyRepository.facet(AptProxyMetadataFacet.class)).thenReturn(proxyMetadataFacet);
    when(aptProxyRepository.facet(AptSigningFacet.class)).thenReturn(signingFacet);
    when(aptProxyRepository.facet(ProxyFacet.class)).thenReturn(proxyFacet);

    when(repositoryManager.get("test-apt-proxy")).thenReturn(aptProxyRepository);
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void testDeltaRebuild_shouldSkipAptKeyValueRepopulation() throws Exception {
    // Configure task for delta rebuild (rebuildAptMetadataFullRebuild=false)
    TaskConfiguration configuration = createTaskConfiguration(false);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify that apt_key_value is NOT repopulated
    verify(hostedKeyValueFacet, never()).removeAllPackageMetadata();
    verify(metadataFacet, never()).addPackageMetadata(any(FluentAsset.class));

    // Verify that metadata files are rebuilt
    verify(metadataFacet, times(1)).rebuildMetadata();
  }

  @Test
  public void testFullRebuild_shouldRepopulateAptKeyValue() throws Exception {
    // Mock some assets for full rebuild
    FluentAsset mockAsset1 = mock(FluentAsset.class);
    FluentAsset mockAsset2 = mock(FluentAsset.class);
    when(hostedContentFacet.getAptPackageAssets()).thenReturn(java.util.Arrays.asList(mockAsset1, mockAsset2));

    // Configure task for full rebuild (rebuildAptMetadataFullRebuild=true)
    TaskConfiguration configuration = createTaskConfiguration(true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify that apt_key_value is cleared
    verify(hostedKeyValueFacet, times(1)).removeAllPackageMetadata();

    // Verify that apt_key_value is repopulated for each asset
    verify(metadataFacet, times(1)).addPackageMetadata(mockAsset1);
    verify(metadataFacet, times(1)).addPackageMetadata(mockAsset2);

    // Verify that metadata files are rebuilt
    verify(metadataFacet, times(1)).rebuildMetadata();
  }

  @Test
  public void testAppliesTo_aptHostedRepository() {
    Repository aptHosted = mock(Repository.class);
    Format aptFormat = new AptFormat();
    Type hostedType = new HostedType();

    when(aptHosted.getFormat()).thenReturn(aptFormat);
    when(aptHosted.getType()).thenReturn(hostedType);

    assertThat(underTest.appliesTo(aptHosted), is(true));
  }

  @Test
  public void testAppliesTo_nonAptRepository() {
    Repository mavenRepo = mock(Repository.class);
    Format mavenFormat = mock(Format.class);
    Type hostedType = new HostedType();

    when(mavenFormat.getValue()).thenReturn("maven");
    when(mavenRepo.getFormat()).thenReturn(mavenFormat);
    when(mavenRepo.getType()).thenReturn(hostedType);

    assertThat(underTest.appliesTo(mavenRepo), is(false));
  }

  @Test
  public void testAppliesTo_aptProxyRepository() {
    Repository aptProxy = mock(Repository.class);
    Format aptFormat = new AptFormat();
    Type proxyType = new ProxyType();

    when(aptProxy.getFormat()).thenReturn(aptFormat);
    when(aptProxy.getType()).thenReturn(proxyType);

    assertThat(underTest.appliesTo(aptProxy), is(true));
  }

  @Test
  public void testProxyRebuild_signingNotConfigured_noReset_shouldSkip() throws Exception {
    // Configure signing as NOT configured (passthrough mode)
    when(signingFacet.isConfigured()).thenReturn(false);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy"));

    TaskConfiguration configuration = createProxyTaskConfiguration(false);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify no rebuild happens when signing is not configured and no reset requested
    verify(proxyMetadataFacet, never()).rebuildMetadata(anyBoolean());
    verify(proxyKeyValueFacet, never()).clearAllTrackedDistributions();
    verify(proxyContentFacet, never()).deleteAssetsByPrefix("dists/");
  }

  @Test
  public void testProxyReset_signingNotConfigured_shouldStillCleanup() throws Exception {
    // Configure signing as NOT configured but reset requested
    // This handles the case of cleaning up stale metadata from previously-enabled signing
    when(signingFacet.isConfigured()).thenReturn(false);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy"));

    TaskConfiguration configuration = createProxyTaskConfiguration(true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify reset happens even without signing
    verify(proxyKeyValueFacet, times(1)).clearAllTrackedDistributions();
    verify(proxyFacet, times(1)).invalidateProxyCaches();

    // Verify rebuild is skipped (no signing configured)
    verify(proxyMetadataFacet, never()).rebuildMetadata(anyBoolean());
  }

  @Test
  public void testProxyRebuild_withoutReset_shouldJustRebuild() throws Exception {
    // Configure signing as configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy"));

    TaskConfiguration configuration = createProxyTaskConfiguration(false);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify no reset happens
    verify(proxyKeyValueFacet, never()).clearAllTrackedDistributions();

    // Verify no re-insert happens (distributions aren't cleared, so no need to re-insert)
    verify(proxyKeyValueFacet, never()).addTrackedDistribution(any());

    // Verify rebuild happens
    verify(proxyMetadataFacet, times(1)).rebuildMetadata(false);
    verify(proxyFacet, never()).invalidateProxyCaches();
  }

  @Test
  public void testProxyRebuild_withReset_shouldClearAndRebuild() throws Exception {
    // Configure signing as configured
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy", "focal"));

    TaskConfiguration configuration = createProxyTaskConfiguration(true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify reset happens - KV cleared
    verify(proxyKeyValueFacet, times(1)).clearAllTrackedDistributions();

    // Verify distributions are re-inserted for rebuild
    verify(proxyKeyValueFacet, times(1)).addTrackedDistribution("jammy");
    verify(proxyKeyValueFacet, times(1)).addTrackedDistribution("focal");

    // Verify rebuild happens
    verify(proxyMetadataFacet, times(1)).rebuildMetadata(true);

    // Verify caches invalidated only during reset
    verify(proxyFacet, times(1)).invalidateProxyCaches();
  }

  @Test
  public void testProxyRebuild_emptyTrackedDistributions_shouldStillRebuild() throws Exception {
    // Configure signing as configured but no tracked distributions
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Collections.emptySet());

    TaskConfiguration configuration = createProxyTaskConfiguration(false);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify no re-insert attempted (no reset, so no re-insert needed)
    verify(proxyKeyValueFacet, never()).addTrackedDistribution(any());

    // Verify rebuild still happens (will process nothing, but should not fail)
    verify(proxyMetadataFacet, times(1)).rebuildMetadata(false);
    verify(proxyFacet, never()).invalidateProxyCaches();
  }

  @Test
  public void testProxyReset_emptyTrackedDistributions_shouldStillResetAndRebuild() throws Exception {
    // Configure signing as configured but no tracked distributions
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Collections.emptySet());

    TaskConfiguration configuration = createProxyTaskConfiguration(true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify reset happens even with no tracked distributions
    verify(proxyKeyValueFacet, times(1)).clearAllTrackedDistributions();

    // Verify no re-insert attempted (empty snapshot)
    verify(proxyKeyValueFacet, never()).addTrackedDistribution(any());

    // Verify rebuild happens
    verify(proxyMetadataFacet, times(1)).rebuildMetadata(true);

    // Verify caches invalidated only during reset
    verify(proxyFacet, times(1)).invalidateProxyCaches();
  }

  @Test
  public void testHostedRebuild_ignoresResetProxyMetadata() throws Exception {
    // Configure hosted repo with proxy-only flag set (should be ignored)
    TaskConfiguration configuration = createTaskConfiguration(false);
    configuration.setBoolean(RebuildAptMetadataTaskDescriptor.APT_PROXY_RESET_METADATA, true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify hosted behavior is unchanged - proxy flag is ignored
    verify(hostedKeyValueFacet, never()).removeAllTrackedDistributions();
    verify(hostedContentFacet, never()).deleteAssetsByPrefix("dists/");
    verify(metadataFacet, times(1)).rebuildMetadata();
  }

  @Test
  public void testProxyRebuild_ignoresFullRebuildFlag() throws Exception {
    // Configure proxy repo with hosted-only flag set (should be ignored)
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy"));

    TaskConfiguration configuration = createProxyTaskConfiguration(false);
    configuration.setBoolean(RebuildAptMetadataTaskDescriptor.APT_METADATA_FULL_REBUILD, true);
    underTest.configure(configuration);

    // Execute
    underTest.call();

    // Verify proxy behavior is unchanged - hosted flag is ignored
    verify(proxyKeyValueFacet, never()).removeAllPackageMetadata(); // hosted-only operation
    verify(proxyMetadataFacet, times(1)).rebuildMetadata(false);
    verify(proxyFacet, never()).invalidateProxyCaches();
  }

  @Test
  public void testProxyRebuild_ioExceptionDuringRebuild_shouldNotThrow() throws Exception {
    // Configure rebuild to throw IOException
    when(signingFacet.isConfigured()).thenReturn(true);
    when(proxyKeyValueFacet.getTrackedDistributions()).thenReturn(Set.of("jammy"));
    when(proxyMetadataFacet.rebuildMetadata(true)).thenThrow(new java.io.IOException("upstream unreachable"));

    TaskConfiguration configuration = createProxyTaskConfiguration(true);
    underTest.configure(configuration);

    // Execute - expect MultipleFailuresException wrapping the runtime failure
    assertThrows(MultipleFailuresException.class, () -> underTest.call());

    // Verify reset still happened before the exception
    verify(proxyKeyValueFacet, times(1)).clearAllTrackedDistributions();
    verify(proxyFacet, times(1)).invalidateProxyCaches();
  }

  private TaskConfiguration createTaskConfiguration(boolean fullRebuild) {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId(UUID.randomUUID().toString());
    configuration.setTypeId(RebuildAptMetadataTaskDescriptor.TYPE_ID);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "test-apt-repo");
    configuration.setBoolean(RebuildAptMetadataTaskDescriptor.APT_METADATA_FULL_REBUILD, fullRebuild);
    return configuration;
  }

  private TaskConfiguration createProxyTaskConfiguration(boolean resetMetadata) {
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId(UUID.randomUUID().toString());
    configuration.setTypeId(RebuildAptMetadataTaskDescriptor.TYPE_ID);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "test-apt-proxy");
    configuration.setBoolean(RebuildAptMetadataTaskDescriptor.APT_PROXY_RESET_METADATA, resetMetadata);
    return configuration;
  }
}
