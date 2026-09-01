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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyRepositoryConfiguration;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.HeaderOnlyPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.apache.http.HttpResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenContentGroupFacetImplTest
{
  private MavenContentGroupFacetImpl underTest;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private Type groupType;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Before
  public void setup() {
    underTest = spy(new MavenContentGroupFacetImpl(repositoryManager, constraintViolationFactory, groupType,
        repositoryCacheInvalidationService));
  }

  /**
   * Ensure that the path used to find assets in the handler uses a path that is
   * prefixed with a "/"
   */
  @Test
  public void testHandleAssetEvent_path_find_with_slash() throws Exception {
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(assetBuilder.find()).thenReturn(Optional.empty());
    FluentAssets assets = mock(FluentAssets.class);
    when(assets.path(any())).thenReturn(assetBuilder);
    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(contentFacet.assets()).thenReturn(assets);
    Repository memberRepo = mock(Repository.class);
    when(memberRepo.getName()).thenReturn("repo1");

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("group-repo");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(groupRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("repo1")).thenReturn(Set.of("group-repo"));
    underTest.attach(groupRepo);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
    when(asset.component()).thenReturn(Optional.empty());
    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(memberRepo));
    underTest.onAssetUploadedEvent(event);
    verify(assets).path("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
  }

  @Test
  public void testCleanupOrphanedGroupAssets_deletesAssetsAndBrowseNodes() throws Exception {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("maven-group");

    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    FluentAsset orphanedAsset1 = mock(FluentAsset.class);
    when(orphanedAsset1.component()).thenReturn(Optional.empty());
    when(orphanedAsset1.path()).thenReturn("/junit/junit/maven-metadata.xml");

    FluentAsset orphanedAsset2 = mock(FluentAsset.class);
    when(orphanedAsset2.component()).thenReturn(Optional.empty());
    when(orphanedAsset2.path()).thenReturn("/junit/junit/maven-metadata.xml.sha1");

    FluentAsset normalAsset = mock(FluentAsset.class);
    when(normalAsset.component()).thenReturn(Optional.of(mock(org.sonatype.nexus.repository.content.Component.class)));

    Continuation<FluentAsset> continuation = mock(Continuation.class);
    when(continuation.nextContinuationToken()).thenReturn(null);
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(continuation);
    doAnswer(invocation -> {
      java.util.function.Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(orphanedAsset1);
      consumer.accept(orphanedAsset2);
      consumer.accept(normalAsset);
      return null;
    }).when(continuation).forEach(any());

    BrowseFacet browseFacet = mock(BrowseFacet.class);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    underTest.attach(repository);

    Set<String> removedMembers = new HashSet<>();
    removedMembers.add("maven-hosted");
    removedMembers.add("maven-proxy");

    underTest.doCleanupOrphanedGroupAssets(removedMembers);

    verify(orphanedAsset1).delete();
    verify(orphanedAsset2).delete();
    verify(normalAsset, never()).delete();
  }

  @Test
  public void testCleanupOrphanedGroupAssets_handlesMissingBrowseFacet() throws Exception {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("maven-group");

    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    FluentAsset orphanedAsset = mock(FluentAsset.class);
    when(orphanedAsset.component()).thenReturn(Optional.empty());
    when(orphanedAsset.path()).thenReturn("/junit/junit/maven-metadata.xml");

    Continuation<FluentAsset> continuation = mock(Continuation.class);
    when(continuation.nextContinuationToken()).thenReturn(null);
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(continuation);
    doAnswer(invocation -> {
      java.util.function.Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(orphanedAsset);
      return null;
    }).when(continuation).forEach(any());

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    underTest.attach(repository);

    Set<String> removedMembers = new HashSet<>();
    removedMembers.add("maven-hosted");

    underTest.doCleanupOrphanedGroupAssets(removedMembers);

    verify(orphanedAsset).delete();
  }

  @Test
  public void testCleanupOrphanedGroupAssets_continuesOnError() throws Exception {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("maven-group");

    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    FluentAsset failingAsset = mock(FluentAsset.class);
    when(failingAsset.component()).thenReturn(Optional.empty());
    when(failingAsset.path()).thenReturn("/junit/junit/maven-metadata.xml");
    when(failingAsset.delete()).thenThrow(new RuntimeException("Delete failed"));

    FluentAsset successAsset = mock(FluentAsset.class);
    when(successAsset.component()).thenReturn(Optional.empty());
    when(successAsset.path()).thenReturn("/org/example/maven-metadata.xml");

    Continuation<FluentAsset> continuation = mock(Continuation.class);
    when(continuation.nextContinuationToken()).thenReturn(null);
    when(fluentAssets.browse(any(Integer.class), any())).thenReturn(continuation);
    doAnswer(invocation -> {
      java.util.function.Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(failingAsset);
      consumer.accept(successAsset);
      return null;
    }).when(continuation).forEach(any());

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    underTest.attach(repository);

    Set<String> removedMembers = new HashSet<>();
    removedMembers.add("maven-hosted");

    underTest.doCleanupOrphanedGroupAssets(removedMembers);

    verify(failingAsset).delete();
    verify(successAsset).delete();
  }

  /**
   * NEXUS-50783: cleanupOrphanedGroupAssets must run the bulk delete on a background thread (not the
   * repository-update thread), and only once the repository is STARTED again. This asserts the worker is invoked
   * asynchronously and off the calling thread.
   */
  @Test
  public void testCleanupOrphanedGroupAssets_runsAsynchronouslyWhenStarted() throws Exception {
    Thread callingThread = Thread.currentThread();
    AtomicReference<Thread> workerThread = new AtomicReference<>();

    // STARTED so the guard passes
    doReturn(true).when(underTest).isStarted();
    // capture the thread the worker runs on; do not touch a real repository/facet
    doAnswer(invocation -> {
      workerThread.set(Thread.currentThread());
      return null;
    }).when(underTest).doCleanupOrphanedGroupAssets(any());

    Set<String> removedMembers = new HashSet<>();
    removedMembers.add("maven-hosted");

    underTest.cleanupOrphanedGroupAssets(removedMembers);

    // worker is invoked (asynchronously - allow time for the background task to run)
    verify(underTest, timeout(5000)).doCleanupOrphanedGroupAssets(removedMembers);
    assertThat(workerThread.get(), notNullValue());
    assertThat("cleanup must not run on the calling (update) thread",
        workerThread.get().equals(callingThread), equalTo(false));
  }

  /**
   * NEXUS-50783: if the repository is not STARTED when the scheduled cleanup runs (e.g. another update or shutdown
   * raced it), the worker must be skipped rather than touching a STOPPED facet.
   * <p>
   * This covers the guard short-circuiting when {@code isStarted()} is false. The narrower race where
   * {@code isStarted()} returns true and the repository then stops mid-scan is handled by
   * {@code doCleanupOrphanedGroupAssets()} swallowing the resulting failure (it is hard to assert deterministically
   * here without real lifecycle wiring).
   */
  @Test
  public void testCleanupOrphanedGroupAssets_skipsWorkerWhenNotStarted() throws Exception {
    doReturn(false).when(underTest).isStarted();

    Set<String> removedMembers = new HashSet<>();
    removedMembers.add("maven-hosted");

    underTest.cleanupOrphanedGroupAssets(removedMembers);

    // Wait the full window before asserting the worker was never invoked. after(...).never() blocks for the duration;
    // timeout(...).times(0) would pass immediately at zero invocations and never actually wait out the async task.
    verify(underTest, after(2000).never()).doCleanupOrphanedGroupAssets(any());
  }

  /**
   * Test that ComponentDeletedEvent properly invalidates group metadata cache
   * when a component is deleted from a member repository.
   */
  @Test
  public void testComponentDeletedEvent_invalidatesGroupMetadata() throws Exception {
    // Setup
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(assetBuilder.find()).thenReturn(Optional.of(fluentAsset));

    FluentAssets assets = mock(FluentAssets.class);
    when(assets.path(any())).thenReturn(assetBuilder);

    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(contentFacet.assets()).thenReturn(assets);

    // Mock Type for the repository
    Type hostedType = mock(Type.class);
    when(hostedType.getValue()).thenReturn("hosted");

    Repository memberRepo = mock(Repository.class);
    when(memberRepo.getName()).thenReturn("hosted-repo");
    when(memberRepo.getType()).thenReturn(hostedType);
    when(memberRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("group-repo");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(groupRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("hosted-repo")).thenReturn(Set.of("group-repo"));
    underTest.attach(groupRepo);

    // Create a mock component with Maven coordinates
    Component component = mock(Component.class);
    when(component.namespace()).thenReturn("com.example");
    when(component.name()).thenReturn("myartifact");
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    when(component.attributes(Maven2Format.NAME)).thenReturn(attributes);
    when(attributes.get(P_BASE_VERSION, String.class)).thenReturn("1.0.0");
    when(component.toStringExternal()).thenReturn("namespace=com.example, name=myartifact, version=1.0.0");

    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getComponent()).thenReturn(component);
    when(event.getRepository()).thenReturn(Optional.of(memberRepo));

    // Execute
    underTest.onComponentDeletedEvent(event);

    // Verify that artifact-level metadata path was marked as stale
    verify(assets).path("/com/example/myartifact/maven-metadata.xml");
    verify(fluentAsset, times(1)).markAsStale();
  }

  /**
   * Test that ComponentDeletedEvent from non-member repository is ignored
   */
  @Test
  public void testComponentDeletedEvent_ignoresProxyRepository() throws Exception {
    // Setup
    FluentAssets assets = mock(FluentAssets.class);
    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.assets()).thenReturn(assets);

    // Mock Type as proxy
    Type proxyType = mock(Type.class);
    when(proxyType.getValue()).thenReturn("proxy");

    Repository proxyRepo = mock(Repository.class);
    when(proxyRepo.getName()).thenReturn("proxy-repo");
    when(proxyRepo.getType()).thenReturn(proxyType);
    when(proxyRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("group-repo");
    when(groupRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("proxy-repo")).thenReturn(Set.of("group-repo"));
    underTest.attach(groupRepo);

    Component component = mock(Component.class);
    when(component.toStringExternal()).thenReturn("test component");

    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getComponent()).thenReturn(component);
    when(event.getRepository()).thenReturn(Optional.of(proxyRepo));

    // Execute
    underTest.onComponentDeletedEvent(event);

    // Verify no metadata invalidation occurred (proxy repos shouldn't trigger invalidation)
    verify(component, never()).namespace();
    verify(component, never()).name();
    verify(assets, never()).path(any());
  }

  /**
   * Test that maybeEvict handles transitive members in nested groups.
   * Scenario: GroupB -> GroupA -> hosted. Upload to hosted should invalidate GroupB's cache.
   */
  @Test
  public void testMaybeEvict_transitiveMember_marksAsStale() throws Exception {
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(assetBuilder.find()).thenReturn(Optional.of(fluentAsset));

    FluentAssets assets = mock(FluentAssets.class);
    when(assets.path(any())).thenReturn(assetBuilder);

    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(contentFacet.assets()).thenReturn(assets);

    // hosted repo is a transitive member of groupB (via groupA)
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getName()).thenReturn("test-hosted");

    Repository groupBRepo = mock(Repository.class);
    when(groupBRepo.getName()).thenReturn("test-groupb");
    when(groupBRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(groupBRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("test-hosted"))
        .thenReturn(Set.of("test-groupa", "test-groupb"));
    underTest.attach(groupBRepo);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
    when(asset.component()).thenReturn(Optional.empty());

    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(hostedRepo));

    underTest.onAssetUploadedEvent(event);

    verify(assets).path("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
    verify(fluentAsset).markAsStale();
  }

  /**
   * Test that ComponentDeletedEvent from a transitive member invalidates metadata.
   */
  @Test
  public void testComponentDeletedEvent_transitiveMember_invalidatesMetadata() throws Exception {
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(assetBuilder.find()).thenReturn(Optional.of(fluentAsset));

    FluentAssets assets = mock(FluentAssets.class);
    when(assets.path(any())).thenReturn(assetBuilder);

    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.assets()).thenReturn(assets);

    Type hostedType = mock(Type.class);
    when(hostedType.getValue()).thenReturn("hosted");

    // hosted repo is a transitive member of groupB (via groupA)
    Repository hostedRepo = mock(Repository.class);
    when(hostedRepo.getName()).thenReturn("test-hosted");
    when(hostedRepo.getType()).thenReturn(hostedType);

    Repository groupBRepo = mock(Repository.class);
    when(groupBRepo.getName()).thenReturn("test-groupb");
    when(groupBRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("test-hosted"))
        .thenReturn(Set.of("test-groupa", "test-groupb"));
    underTest.attach(groupBRepo);

    Component component = mock(Component.class);
    when(component.namespace()).thenReturn("com.example");
    when(component.name()).thenReturn("myartifact");
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    when(component.attributes(Maven2Format.NAME)).thenReturn(attributes);
    when(attributes.get(P_BASE_VERSION, String.class)).thenReturn("1.0.0");
    when(component.toStringExternal()).thenReturn("namespace=com.example, name=myartifact, version=1.0.0");

    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    when(event.getComponent()).thenReturn(component);
    when(event.getRepository()).thenReturn(Optional.of(hostedRepo));

    underTest.onComponentDeletedEvent(event);

    verify(assets).path("/com/example/myartifact/maven-metadata.xml");
    verify(fluentAsset).markAsStale();
  }

  /**
   * Test that events from non-member repositories (neither direct nor transitive) are ignored.
   */
  @Test
  public void testMaybeEvict_nonMember_doesNotEvict() throws Exception {
    FluentAssets assets = mock(FluentAssets.class);
    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(contentFacet.assets()).thenReturn(assets);

    Repository unrelatedRepo = mock(Repository.class);
    when(unrelatedRepo.getName()).thenReturn("unrelated-repo");

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("test-group");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(groupRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    when(repositoryManager.findContainingGroups("unrelated-repo"))
        .thenReturn(new HashSet<>(Set.of("some-other-group"))); // in a group, but not this one
    underTest.attach(groupRepo);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/com/example/foo/maven-metadata.xml");
    when(asset.component()).thenReturn(Optional.empty());

    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(unrelatedRepo));

    underTest.onAssetUploadedEvent(event);

    verify(assets, never()).path(any());
  }

  // ---- computeMinProxyMetadataMaxAgeSeconds tests (NEXUS-52362) ----

  @Test
  public void computeMinProxyMetadataMaxAge_noLeafMembers_returnsNegative() {
    doReturn(List.of()).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(-1));
  }

  @Test
  public void computeMinProxyMetadataMaxAge_hostedMembersOnly_returnsNegative() {
    Repository hostedMember = mock(Repository.class);
    when(hostedMember.optionalFacet(ProxyFacet.class)).thenReturn(Optional.empty());

    doReturn(List.of(hostedMember)).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(-1));
  }

  @Test
  public void computeMinProxyMetadataMaxAge_proxyWithZeroMaxAge_returnsZero() {
    ProxyRepositoryConfiguration config = mock(ProxyRepositoryConfiguration.class);
    when(config.getMetadataMaxAge()).thenReturn(Duration.ofMinutes(0));

    ProxyFacet proxyFacet = mock(ProxyFacet.class);
    when(proxyFacet.getConfiguration()).thenReturn(config);

    Repository proxyMember = mock(Repository.class);
    when(proxyMember.optionalFacet(ProxyFacet.class)).thenReturn(Optional.of(proxyFacet));

    doReturn(List.of(proxyMember)).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(0));
  }

  @Test
  public void computeMinProxyMetadataMaxAge_multipleProxies_returnsMinimum() {
    Repository proxy1 = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(30));
    Repository proxy2 = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(5));
    Repository proxy3 = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(60));

    doReturn(List.of(proxy1, proxy2, proxy3)).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(300)); // 5 minutes = 300s
  }

  @Test
  public void computeMinProxyMetadataMaxAge_proxyWithNegativeMaxAge_returnsNegative() {
    // negative metadataMaxAge means "never expire" — should be excluded
    Repository proxy = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(-1));

    doReturn(List.of(proxy)).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(-1));
  }

  @Test
  public void computeMinProxyMetadataMaxAge_mixedFiniteAndInfinite_returnsFiniteMinimum() {
    Repository proxy1 = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(-1)); // infinite
    Repository proxy2 = proxyMemberWithMetadataMaxAge(Duration.ofMinutes(10));

    doReturn(List.of(proxy1, proxy2)).when(underTest).leafMembers();

    assertThat(underTest.computeMinProxyMetadataMaxAgeSeconds(), equalTo(600)); // 10 minutes = 600s
  }

  @Test
  public void getCached_returnsNull_whenProxyMetadataMaxAgeIsZero() throws Exception {
    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();
    MavenPath mavenPath = pathParser.parsePath("org/apache/commons/commons-lang3/maven-metadata.xml");

    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(pathParser);

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(fluentAssets.path(any())).thenReturn(assetBuilder);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);

    org.sonatype.nexus.repository.view.Content content = mock(org.sonatype.nexus.repository.view.Content.class);
    when(content.getSize()).thenReturn(512L);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.download()).thenReturn(content);
    // first isStale() call is the group's infinite-TTL cacheController → fresh
    // second isStale() call is the proxy TTL CacheController(0,...) → stale
    when(asset.isStale(any())).thenReturn(false, true);
    when(assetBuilder.find()).thenReturn(Optional.of(asset));

    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-group");
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(repository);

    // proxy metadataMaxAge=0 → always stale; group must not serve cached merged result
    underTest.minProxyMetadataMaxAgeSeconds = 0; // bypass lazy init, set directly

    assertThat(underTest.getCached(mavenPath), nullValue());
  }

  @Test
  public void getCached_returnsContent_whenProxyMetadataMaxAgeIsNegative() throws Exception {
    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();
    MavenPath mavenPath = pathParser.parsePath("org/apache/commons/commons-lang3/maven-metadata.xml");

    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(pathParser);

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(fluentAssets.path(any())).thenReturn(assetBuilder);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);

    org.sonatype.nexus.repository.view.Content content = mock(org.sonatype.nexus.repository.view.Content.class);
    when(content.getSize()).thenReturn(512L);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.download()).thenReturn(content);
    when(asset.isStale(any())).thenReturn(false); // group's cacheController says fresh
    when(assetBuilder.find()).thenReturn(Optional.of(asset));

    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-group");
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(repository);

    // no proxy has a finite TTL (hosted-only group) → existing infinite-TTL behaviour preserved
    underTest.minProxyMetadataMaxAgeSeconds = -1; // bypass lazy init, set directly

    assertThat(underTest.getCached(mavenPath), notNullValue());
  }

  private static Repository proxyMemberWithMetadataMaxAge(final Duration metadataMaxAge) {
    ProxyRepositoryConfiguration config = mock(ProxyRepositoryConfiguration.class);
    when(config.getMetadataMaxAge()).thenReturn(metadataMaxAge);

    ProxyFacet proxyFacet = mock(ProxyFacet.class);
    when(proxyFacet.getConfiguration()).thenReturn(config);

    Repository member = mock(Repository.class);
    when(member.optionalFacet(ProxyFacet.class)).thenReturn(Optional.of(proxyFacet));
    return member;
  }

  /**
   * Regression test for NEXUS-47909: {@code getCached} must accept archetype-catalog.xml hash paths
   * (e.g. {@code archetype-catalog.xml.sha1}) without throwing {@code IllegalArgumentException} from
   * {@code checkMergeHandled}. Prior to the fix, the guard compared {@code mavenPath.getFileName()}
   * directly to the constant, which fails for subordinate hash paths. The guard now normalizes via
   * {@code mavenPath.main().getFileName()}, matching the symmetric check used for repository metadata.
   */
  @Test
  public void getCached_archetypeCatalogHashPaths_doesNotThrow() throws Exception {
    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();

    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(pathParser);

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(fluentAssets.path(any())).thenReturn(assetBuilder);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);

    org.sonatype.nexus.repository.view.Content content = mock(org.sonatype.nexus.repository.view.Content.class);
    when(content.getSize()).thenReturn(40L);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.download()).thenReturn(content);
    when(assetBuilder.find()).thenReturn(Optional.of(asset));

    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-group");
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(repository);

    for (String suffix : new String[]{".sha1", ".md5", ".sha256", ".sha512"}) {
      MavenPath hashPath = pathParser.parsePath("archetype-catalog.xml" + suffix);
      // Must not throw IllegalArgumentException from checkMergeHandled; hash paths bypass staleness
      // checks and return the cached hash content directly.
      assertThat(underTest.getCached(hashPath), notNullValue());
    }
  }

  /**
   * Companion check for NEXUS-47909: the main {@code archetype-catalog.xml} path itself must still be
   * accepted by {@code checkMergeHandled}. This guards against regressions that would restrict the
   * guard too aggressively (e.g. forgetting the {@code .main()} call entirely).
   */
  @Test
  public void getCached_archetypeCatalogMainPath_doesNotThrow() throws Exception {
    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();

    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(pathParser);

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(fluentAssets.path(any())).thenReturn(assetBuilder);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);
    when(assetBuilder.find()).thenReturn(Optional.empty());

    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-group");
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(repository);

    MavenPath mainPath = pathParser.parsePath("archetype-catalog.xml");
    // cache miss returns null, but no exception from the merge-handled guard
    assertThat(underTest.getCached(mainPath), nullValue());
  }

  /**
   * NEXUS-53780 (regression coverage): a member response whose payload is a
   * {@link HeaderOnlyPayload} carries no body. It must not be fed to the maven-metadata merger,
   * because merging an empty stream truncates the merged result and poisons the group cache.
   * When mixed with a real body-bearing member, the merged output must reflect only the real
   * member's content — no truncation from the body-less one.
   */
  @Test
  public void merge_skipsHeaderOnlyPayloadMembers() throws Exception {
    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("maven-group");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(groupRepo);

    Repository memberA = mock(Repository.class);
    when(memberA.getName()).thenReturn("member-a-head-only");
    Repository memberB = mock(Repository.class);
    when(memberB.getName()).thenReturn("member-b-real");

    // memberA: body-less HEAD response (would have poisoned the merge)
    Response headerOnlyResponse = okResponseWith(new Content(new HeaderOnlyPayload(mock(HttpResponse.class))));
    // memberB: real maven-metadata payload with a single version
    String memberBMetadata = ""
        + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<metadata>"
        + "  <groupId>com.example</groupId>"
        + "  <artifactId>nexus-test</artifactId>"
        + "  <versioning>"
        + "    <versions>"
        + "      <version>2.0.1</version>"
        + "    </versions>"
        + "  </versioning>"
        + "</metadata>";
    Response realResponse = okResponseWith(new Content(new StringPayload(memberBMetadata, "text/xml")));

    LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
    responses.put(memberA, headerOnlyResponse);
    responses.put(memberB, realResponse);

    MavenPath mavenPath = new Maven2MavenPathParser()
        .parsePath("/com/example/nexus-test/maven-metadata.xml");

    Content merged = underTest.mergeWithoutCaching(mavenPath, responses);

    assertThat("merger must produce output from the real member",
        merged, notNullValue());
    String mergedXml = new String(merged.openInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertThat("merged output must include memberB's version",
        mergedXml.contains("<version>2.0.1</version>"), equalTo(true));
  }

  /**
   * NEXUS-53780 (regression coverage): when every 200-OK member response is body-less
   * (all-HEAD scenario), the merger must return {@code null} rather than a corrupt merged blob.
   * This is the safety net for the exact failure mode the customer hit: HEAD dispatched to
   * every member on a stale group cache resulted in a poisoned cached blob.
   */
  @Test
  public void merge_returnsNullWhenAllMembersAreHeaderOnly() throws Exception {
    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("maven-group");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(groupRepo);

    Repository memberA = mock(Repository.class);
    when(memberA.getName()).thenReturn("member-a");
    Repository memberB = mock(Repository.class);
    when(memberB.getName()).thenReturn("member-b");

    Response headOnlyA = okResponseWith(new Content(new HeaderOnlyPayload(mock(HttpResponse.class))));
    Response headOnlyB = okResponseWith(new Content(new HeaderOnlyPayload(mock(HttpResponse.class))));

    LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
    responses.put(memberA, headOnlyA);
    responses.put(memberB, headOnlyB);

    MavenPath mavenPath = new Maven2MavenPathParser()
        .parsePath("/com/example/nexus-test/maven-metadata.xml");

    Content merged = underTest.mergeWithoutCaching(mavenPath, responses);

    // No real content contributed to the merge — return null instead of a poisoned blob.
    assertThat(merged, nullValue());
  }

  /**
   * NEXUS-53858 (regression coverage): when every member returns unparseable maven-metadata
   * (e.g. an HTML error page), the merger produces zero bytes. The resulting zero-size
   * {@code TempBlob} must not be stored via {@code put()} — that would fail Maven metadata
   * validation ({@code InvalidContentException}) and spam WARN logs on every request. The
   * guard in {@code cache()} must detect the zero-size blob, skip {@code put()}, and serve
   * an empty 200 body uncached so the group stays functional without log pollution.
   */
  @Test
  public void mergeAndCache_emptyMergedBlob_skipsPutAndServesUncached() throws Exception {
    MavenContentFacet mavenContentFacet = mock(MavenContentFacet.class);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());

    // Mock the blobs chain so createTempBlob returns a zero-size TempBlob
    FluentBlobs blobsMock = mock(FluentBlobs.class);
    when(mavenContentFacet.blobs()).thenReturn(blobsMock);

    TempBlob tempBlob = mock(TempBlob.class);
    Blob blob = mock(Blob.class);
    BlobMetrics metrics = mock(BlobMetrics.class);
    when(metrics.getContentSize()).thenReturn(0L);
    when(blob.getMetrics()).thenReturn(metrics);
    when(tempBlob.getBlob()).thenReturn(blob);
    when(tempBlob.get()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(blobsMock.ingest(any(InputStream.class), any(), any())).thenReturn(tempBlob);

    // Assets chain used by serveUncached to mark any existing cached asset as stale
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(assetBuilder.find()).thenReturn(Optional.empty());
    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(fluentAssets.path(any())).thenReturn(assetBuilder);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);

    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("maven-group");
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(mavenContentFacet);
    underTest.attach(repository);

    // Both members return unparseable content (HTML instead of maven-metadata XML),
    // so the merger writes nothing and the resulting TempBlob is empty.
    Repository memberA = mock(Repository.class);
    when(memberA.getName()).thenReturn("member-a");
    Repository memberB = mock(Repository.class);
    when(memberB.getName()).thenReturn("member-b");

    Response responseA = okResponseWith(new Content(new StringPayload("<html>not metadata</html>", "text/html")));
    Response responseB = okResponseWith(new Content(new StringPayload("<html>not metadata</html>", "text/html")));

    LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
    responses.put(memberA, responseA);
    responses.put(memberB, responseB);

    MavenPath mavenPath = new Maven2MavenPathParser().parsePath("/com/example/nexus-test/maven-metadata.xml");

    Content result = underTest.mergeAndCache(mavenPath, responses);

    assertThat(result, notNullValue());
    assertThat(result.openInputStream().readAllBytes().length, equalTo(0));
    // Key regression assertion: empty merged blob must never be put() — no InvalidContentException, no WARN spam
    verify(mavenContentFacet, never()).put(any(), any());
  }

  private static Response okResponseWith(final Content payload) {
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(Status.success(200));
    when(response.getPayload()).thenReturn(payload);
    return response;
  }
}
