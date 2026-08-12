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

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
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
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyRepositoryConfiguration;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;

public class MavenContentGroupFacetImplTest
    extends TestSupport
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
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("repo1");
    when(repository.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    doReturn(true).when(underTest).member(repository);
    underTest.attach(repository);
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
    when(asset.component()).thenReturn(Optional.empty());
    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));
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

    underTest.cleanupOrphanedGroupAssets(removedMembers);

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

    underTest.cleanupOrphanedGroupAssets(removedMembers);

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

    underTest.cleanupOrphanedGroupAssets(removedMembers);

    verify(failingAsset).delete();
    verify(successAsset).delete();
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
    when(memberRepo.getType()).thenReturn(hostedType); // This was missing!
    when(memberRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("group-repo");
    when(groupRepo.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(groupRepo.facet(ContentFacet.class)).thenReturn(contentFacet);

    doReturn(true).when(underTest).member(memberRepo);
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

    doReturn(true).when(underTest).member(proxyRepo); // It's a member but proxy type
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
}
