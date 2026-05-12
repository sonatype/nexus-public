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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
}
