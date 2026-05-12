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
package org.sonatype.nexus.repository.content.browse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeDAO;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeStore;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BrowseFacetImplTest
{
  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private BrowseNodeStore<BrowseNodeDAO> browseNodeStore;

  @Mock
  private BrowseNodeGenerator browseNodeGenerator;

  @Mock
  private BrowseNodeManager browseNodeManager;

  @Mock
  private Repository repository;

  @Mock(answer = Answers.RETURNS_MOCKS)
  private ContentFacetSupport contentFacet;

  @Mock
  private MockedStatic<QualifierUtil> qualifierUtil;

  @Mock
  private FluentAssets fluentAssets;

  private BrowseFacetImpl underTest;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    when(formatStoreManager.formatStore(any(), eq(BrowseNodeDAO.class))).thenReturn(browseNodeStore);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(Map.of("raw", formatStoreManager), Map.of("raw", browseNodeGenerator));

    underTest = new BrowseFacetImpl(
        List.of(), List.of(),
        1000);
    underTest.installDependencies(mock(EventManager.class));

    when(repository.getFormat()).thenReturn(new Format("raw")
    {
    });
    when(repository.getName()).thenReturn("My-Raw-Repository");

    underTest.attach(repository);
    underTest.init();
    underTest.start();

    Field browseNodeManagerField = BrowseFacetImpl.class.getDeclaredField("browseNodeManager");
    browseNodeManagerField.setAccessible(true);
    browseNodeManagerField.set(underTest, browseNodeManager);

    Field browseNodeGeneratorField = BrowseFacetImpl.class.getDeclaredField("browseNodeGenerator");
    browseNodeGeneratorField.setAccessible(true);
    browseNodeGeneratorField.set(underTest, browseNodeGenerator);
  }

  // --- deleteByAssetIdAndPath tests ---

  @Test
  public void testDeleteByAssetIdAndPath() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(List.of());

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, never()).delete(anyLong());
  }

  @Test
  public void testDeleteByAssetIdAndPathWithParentNode() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;
    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(0L);
    parentNode.setNodeId(parentNodeId);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Arrays.asList(parentNode));

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndPathWithParentNodeWithAssets() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;
    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(10L);
    parentNode.setNodeId(parentNodeId);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Arrays.asList(parentNode));

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, never()).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndParentWithChildIsNotDeleted() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;

    BrowseNodeData childNode = new BrowseNodeData();
    childNode.setAssetCount(0L);
    childNode.setNodeId(100L);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(generateParentNodes());

    when(browseNodeManager.hasAnyAssetOrComponentChildren(3L)).thenReturn(true);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager, times(1)).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, times(1)).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndPath_fallsBackToDeleteByAssetId() {
    Integer internalAssetId = 5;
    String path = "some/path";
    Long fallbackParentNodeId = 10L;

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(null);
    when(browseNodeManager.deleteByAssetId(internalAssetId)).thenReturn(fallbackParentNodeId);
    when(browseNodeManager.getNodeParents(fallbackParentNodeId)).thenReturn(List.of());

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager).deleteByAssetId(internalAssetId);
    verify(browseNodeManager).getNodeParents(fallbackParentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndPath_bothDeleteMethodsReturnNull() {
    Integer internalAssetId = 5;
    String path = "some/path";

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(null);
    when(browseNodeManager.deleteByAssetId(internalAssetId)).thenReturn(null);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager).deleteByAssetId(internalAssetId);
    verify(browseNodeManager, never()).getNodeParents(anyLong());
    verify(browseNodeManager, never()).delete(anyLong());
  }

  @Test
  public void testDeleteByAssetIdAndPath_nullParentNodeInList() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);

    BrowseNodeData nodeAfterNull = new BrowseNodeData();
    nodeAfterNull.setAssetCount(0L);
    nodeAfterNull.setNodeId(5L);

    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Arrays.asList(null, nodeAfterNull));
    when(browseNodeManager.hasAnyAssetOrComponentChildren(5L)).thenReturn(false);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).delete(5L);
  }

  @Test
  public void testDeleteByAssetIdAndPath_parentWithNullAssetCountTreatedAsZero() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;
    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setNodeId(parentNodeId);
    // do not call setAssetCount - leaves it as null

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(List.of(parentNode));
    when(browseNodeManager.hasAnyAssetOrComponentChildren(parentNodeId)).thenReturn(false);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    // null asset count treated as 0, so it should proceed to check children and delete
    verify(browseNodeManager).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndPath_multipleParentsDeletedUntilChildrenFound() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 10L;

    BrowseNodeData node1 = new BrowseNodeData();
    node1.setAssetCount(0L);
    node1.setNodeId(10L);

    BrowseNodeData node2 = new BrowseNodeData();
    node2.setAssetCount(0L);
    node2.setNodeId(20L);

    BrowseNodeData node3 = new BrowseNodeData();
    node3.setAssetCount(0L);
    node3.setNodeId(30L);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(List.of(node1, node2, node3));
    when(browseNodeManager.hasAnyAssetOrComponentChildren(10L)).thenReturn(false);
    when(browseNodeManager.hasAnyAssetOrComponentChildren(20L)).thenReturn(false);
    when(browseNodeManager.hasAnyAssetOrComponentChildren(30L)).thenReturn(true);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).delete(10L);
    verify(browseNodeManager).delete(20L);
    verify(browseNodeManager, never()).delete(30L);
  }

  @Test
  public void testDeleteByAssetIdAndPath_stopsAtNodeWithAssets() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 10L;

    BrowseNodeData node1 = new BrowseNodeData();
    node1.setAssetCount(0L);
    node1.setNodeId(10L);

    BrowseNodeData node2 = new BrowseNodeData();
    node2.setAssetCount(5L);
    node2.setNodeId(20L);

    BrowseNodeData node3 = new BrowseNodeData();
    node3.setAssetCount(0L);
    node3.setNodeId(30L);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(List.of(node1, node2, node3));
    when(browseNodeManager.hasAnyAssetOrComponentChildren(10L)).thenReturn(false);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).delete(10L);
    // node2 has assetCount > 0, so we stop
    verify(browseNodeManager, never()).delete(20L);
    verify(browseNodeManager, never()).delete(30L);
  }

  // --- getByDisplayPath tests ---

  @Test
  public void testGetByDisplayPath_delegatesToManager() {
    List<String> displayPath = List.of("org", "sonatype");
    int limit = 50;
    String filter = "name LIKE '%test%'";
    Map<String, Object> filterParams = Map.of("name", "test");

    List<BrowseNode> expectedNodes = List.of(mock(BrowseNode.class));
    when(browseNodeManager.getByDisplayPath(displayPath, limit, filter, filterParams)).thenReturn(expectedNodes);

    List<BrowseNode> result = underTest.getByDisplayPath(displayPath, limit, filter, filterParams);

    assertThat(result, is(expectedNodes));
    verify(browseNodeManager).getByDisplayPath(displayPath, limit, filter, filterParams);
  }

  @Test
  public void testGetByDisplayPath_withNullFilterAndParams() {
    List<String> displayPath = List.of("root");
    int limit = 10;

    when(browseNodeManager.getByDisplayPath(displayPath, limit, null, null)).thenReturn(emptyList());

    List<BrowseNode> result = underTest.getByDisplayPath(displayPath, limit, null, null);

    assertThat(result, is(empty()));
    verify(browseNodeManager).getByDisplayPath(displayPath, limit, null, null);
  }

  // --- trimBrowseNodes tests ---

  @Test
  public void testTrimBrowseNodes_delegatesToManager() {
    underTest.trimBrowseNodes();

    verify(browseNodeManager).trimBrowseNodes();
  }

  // --- deleteByNodeId tests ---

  @Test
  public void testDeleteByNodeId_delegatesToManager() {
    Long nodeId = 42L;

    underTest.deleteByNodeId(nodeId);

    verify(browseNodeManager).delete(nodeId);
  }

  // --- getByRequestPath tests ---

  @Test
  public void testGetByRequestPath_returnsNodeWhenFound() {
    String requestPath = "/org/sonatype/artifact";
    BrowseNode expectedNode = mock(BrowseNode.class);
    when(browseNodeManager.getByRequestPath(requestPath)).thenReturn(expectedNode);

    Optional<BrowseNode> result = underTest.getByRequestPath(requestPath);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(expectedNode));
  }

  @Test
  public void testGetByRequestPath_returnsEmptyWhenNotFound() {
    String requestPath = "/does/not/exist";
    when(browseNodeManager.getByRequestPath(requestPath)).thenReturn(null);

    Optional<BrowseNode> result = underTest.getByRequestPath(requestPath);

    assertThat(result.isPresent(), is(false));
  }

  // --- rebuildBrowseNodes tests ---

  @Test
  public void testRebuildBrowseNodes_deletesAndRebuilds() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(0);

    @SuppressWarnings("unchecked")
    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(fluentAssets.browse(anyInt(), isNull())).thenReturn(emptyPage);

    Consumer<String> progressUpdater = mock(Consumer.class);

    underTest.rebuildBrowseNodes(progressUpdater);

    verify(browseNodeManager).deleteBrowseNodes();
    // With count == 0, no asset browsing should happen
    verify(fluentAssets, never()).browse(anyInt(), isNull());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_processesAssetsWithProgress() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(2);

    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset2 = mock(FluentAsset.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(2);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset1);
      consumer.accept(asset2);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(fluentAssets.browse(eq(1000), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(emptyPage);

    // browseNodeGenerator needed for createBrowseNodes
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(any())).thenReturn(emptyList());

    Consumer<String> progressUpdater = mock(Consumer.class);

    underTest.rebuildBrowseNodes(progressUpdater);

    verify(browseNodeManager).deleteBrowseNodes();
    verify(progressUpdater).accept("100% Complete");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_withNullProgressUpdater() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(1);

    FluentAsset asset = mock(FluentAsset.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(1);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(fluentAssets.browse(eq(1000), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(emptyPage);

    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(any())).thenReturn(emptyList());

    // Should not throw NPE with null progress updater
    underTest.rebuildBrowseNodes(null);

    verify(browseNodeManager).deleteBrowseNodes();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_multiplePages() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(3);

    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset2 = mock(FluentAsset.class);
    FluentAsset asset3 = mock(FluentAsset.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(2);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset1);
      consumer.accept(asset2);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> secondPage = mock(Continuation.class);
    when(secondPage.isEmpty()).thenReturn(false);
    when(secondPage.size()).thenReturn(1);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset3);
      return null;
    }).when(secondPage).forEach(any());
    when(secondPage.nextContinuationToken()).thenReturn("token2");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(fluentAssets.browse(eq(1000), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(secondPage);
    when(fluentAssets.browse(eq(1000), eq("token2"))).thenReturn(emptyPage);

    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(any())).thenReturn(emptyList());

    Consumer<String> progressUpdater = mock(Consumer.class);

    underTest.rebuildBrowseNodes(progressUpdater);

    verify(browseNodeManager).deleteBrowseNodes();
    // Progress should be reported for each page
    verify(progressUpdater, times(2)).accept(anyString());
  }

  // --- addPathsToAssets tests ---

  @Test
  public void testAddPathsToAssets_emptyCollection() {
    underTest.addPathsToAssets(emptyList());

    verify(browseNodeManager, never()).hasAssetNode(any());
  }

  @Test
  public void testAddPathsToAssets_assetNotFound() {
    EntityId assetId = mock(EntityId.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.empty());

    underTest.addPathsToAssets(List.of(assetId));

    verify(browseNodeManager, never()).hasAssetNode(any());
  }

  @Test
  public void testAddPathsToAssets_assetAlreadyHasBrowseNode() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(true);

    underTest.addPathsToAssets(List.of(assetId));

    // Should not create browse nodes for assets that already have nodes
    verify(browseNodeGenerator, never()).computeAssetPaths(any());
  }

  @Test
  public void testAddPathsToAssets_createsNodesForNewAsset_singleAssetPerComponent() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(emptyList());

    underTest.addPathsToAssets(List.of(assetId));

    verify(browseNodeGenerator).hasMultipleAssetsPerComponent();
    verify(browseNodeGenerator).computeAssetPaths(asset);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_singleAssetPerComponent_createsNodesWithAssetAndComponent() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);
    Component component = mock(Component.class);

    BrowsePath browsePath = mock(BrowsePath.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(browsePath));
    when(asset.component()).thenReturn(Optional.of(component));

    underTest.addPathsToAssets(List.of(assetId));

    // For single-asset-per-component, createCombinedAssetAndComponentBrowseNodes is called
    verify(browseNodeManager).createBrowseNodes(eq(List.of(browsePath)), any(Consumer.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_multipleAssetsPerComponent_createsAssetAndComponentNodes() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);
    Component component = mock(Component.class);

    BrowsePath assetPath = mock(BrowsePath.class);
    BrowsePath componentPath = mock(BrowsePath.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(assetPath));
    when(browseNodeGenerator.computeComponentPaths(asset)).thenReturn(List.of(componentPath));
    when(asset.component()).thenReturn(Optional.of(component));

    // Mock InternalIds.internalComponentId via static mock
    try (MockedStatic<org.sonatype.nexus.repository.content.store.InternalIds> internalIdsMock =
        org.mockito.Mockito.mockStatic(org.sonatype.nexus.repository.content.store.InternalIds.class)) {
      internalIdsMock.when(() -> org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId(component))
          .thenReturn(42);

      underTest.addPathsToAssets(List.of(assetId));
    }

    // Should have created asset browse nodes and component browse nodes separately
    verify(browseNodeManager, times(2)).createBrowseNodes(anyList(), any(Consumer.class));
    verify(browseNodeGenerator).computeAssetPaths(asset);
    verify(browseNodeGenerator).computeComponentPaths(asset);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_multipleAssetsPerComponent_assetWithoutComponent() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);

    BrowsePath assetPath = mock(BrowsePath.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(assetPath));
    when(asset.component()).thenReturn(Optional.empty());

    underTest.addPathsToAssets(List.of(assetId));

    // Asset nodes created but no component nodes since asset has no component
    verify(browseNodeManager, times(1)).createBrowseNodes(eq(List.of(assetPath)), any(Consumer.class));
    verify(browseNodeGenerator, never()).computeComponentPaths(any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_multipleAssetsPerComponent_emptyAssetPaths() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(emptyList());
    when(asset.component()).thenReturn(Optional.empty());

    underTest.addPathsToAssets(List.of(assetId));

    // No browse nodes should be created when paths are empty
    verify(browseNodeManager, never()).createBrowseNodes(anyList(), any(Consumer.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_multipleAssetsPerComponent_emptyComponentPaths() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);
    Component component = mock(Component.class);

    BrowsePath assetPath = mock(BrowsePath.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(assetPath));
    when(browseNodeGenerator.computeComponentPaths(asset)).thenReturn(emptyList());
    when(asset.component()).thenReturn(Optional.of(component));

    try (MockedStatic<org.sonatype.nexus.repository.content.store.InternalIds> internalIdsMock =
        org.mockito.Mockito.mockStatic(org.sonatype.nexus.repository.content.store.InternalIds.class)) {
      internalIdsMock.when(() -> org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId(component))
          .thenReturn(42);

      underTest.addPathsToAssets(List.of(assetId));
    }

    // Asset nodes created but component browse nodes not created due to empty paths
    verify(browseNodeManager, times(1)).createBrowseNodes(eq(List.of(assetPath)), any(Consumer.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_multipleAssetsPerComponent_componentDeduplication() {
    // Two assets share the same component; component browse nodes should only be created once
    EntityId assetId1 = mock(EntityId.class);
    EntityId assetId2 = mock(EntityId.class);
    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset2 = mock(FluentAsset.class);
    Component component = mock(Component.class);

    BrowsePath assetPath1 = mock(BrowsePath.class);
    BrowsePath assetPath2 = mock(BrowsePath.class);
    BrowsePath componentPath = mock(BrowsePath.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId1)).thenReturn(Optional.of(asset1));
    when(fluentAssets.find(assetId2)).thenReturn(Optional.of(asset2));
    when(browseNodeManager.hasAssetNode(asset1)).thenReturn(false);
    when(browseNodeManager.hasAssetNode(asset2)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset1)).thenReturn(List.of(assetPath1));
    when(browseNodeGenerator.computeAssetPaths(asset2)).thenReturn(List.of(assetPath2));
    when(browseNodeGenerator.computeComponentPaths(asset1)).thenReturn(List.of(componentPath));
    when(browseNodeGenerator.computeComponentPaths(asset2)).thenReturn(List.of(componentPath));
    when(asset1.component()).thenReturn(Optional.of(component));
    when(asset2.component()).thenReturn(Optional.of(component));

    try (MockedStatic<org.sonatype.nexus.repository.content.store.InternalIds> internalIdsMock =
        org.mockito.Mockito.mockStatic(org.sonatype.nexus.repository.content.store.InternalIds.class)) {
      internalIdsMock.when(() -> org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId(component))
          .thenReturn(42);

      underTest.addPathsToAssets(List.of(assetId1, assetId2));
    }

    // Both asset paths should be created
    verify(browseNodeGenerator).computeAssetPaths(asset1);
    verify(browseNodeGenerator).computeAssetPaths(asset2);
    // Component paths should only be computed once due to caching
    verify(browseNodeGenerator, times(1)).computeComponentPaths(any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_singleAssetPerComponent_emptyPaths() {
    EntityId assetId = mock(EntityId.class);
    FluentAsset asset = mock(FluentAsset.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(asset));
    when(browseNodeManager.hasAssetNode(asset)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(emptyList());

    underTest.addPathsToAssets(List.of(assetId));

    verify(browseNodeManager, never()).createBrowseNodes(anyList(), any(Consumer.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddPathsToAssets_mixOfFoundAndNotFound() {
    EntityId assetId1 = mock(EntityId.class);
    EntityId assetId2 = mock(EntityId.class);
    EntityId assetId3 = mock(EntityId.class);

    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset3 = mock(FluentAsset.class);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId1)).thenReturn(Optional.of(asset1));
    when(fluentAssets.find(assetId2)).thenReturn(Optional.empty());
    when(fluentAssets.find(assetId3)).thenReturn(Optional.of(asset3));
    when(browseNodeManager.hasAssetNode(asset1)).thenReturn(true);
    when(browseNodeManager.hasAssetNode(asset3)).thenReturn(false);
    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(asset3)).thenReturn(emptyList());

    underTest.addPathsToAssets(List.of(assetId1, assetId2, assetId3));

    // asset1 already has node, asset2 not found - neither should generate paths
    verify(browseNodeGenerator, never()).computeAssetPaths(asset1);
    // asset3 found and needs node
    verify(browseNodeGenerator).computeAssetPaths(asset3);
  }

  // --- rebuildBrowseNodes with createBrowseNodes path tests ---

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_singleAssetPerComponent_withPaths() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(1);

    FluentAsset asset = mock(FluentAsset.class);
    Component component = mock(Component.class);
    BrowsePath path = mock(BrowsePath.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(1);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(fluentAssets.browse(eq(1000), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(emptyPage);

    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(path));
    when(asset.component()).thenReturn(Optional.of(component));

    underTest.rebuildBrowseNodes(mock(Consumer.class));

    verify(browseNodeManager).deleteBrowseNodes();
    verify(browseNodeManager).createBrowseNodes(eq(List.of(path)), any(Consumer.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_multipleAssetsPerComponent_withPaths() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(1);

    FluentAsset asset = mock(FluentAsset.class);
    BrowsePath assetPath = mock(BrowsePath.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(1);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(fluentAssets.browse(eq(1000), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(emptyPage);

    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(true);
    when(browseNodeGenerator.computeAssetPaths(asset)).thenReturn(List.of(assetPath));
    when(asset.component()).thenReturn(Optional.empty());

    underTest.rebuildBrowseNodes(mock(Consumer.class));

    verify(browseNodeManager).deleteBrowseNodes();
    // Only asset nodes created (no component)
    verify(browseNodeManager, times(1)).createBrowseNodes(eq(List.of(assetPath)), any(Consumer.class));
  }

  // --- constructor pageSize validation ---

  @SuppressWarnings("unchecked")
  @Test
  public void testPageSizeEnforcedToMinimumOfOne() throws Exception {
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(Map.of("raw", formatStoreManager), Map.of("raw", browseNodeGenerator));

    // Create with pageSize of 0 - should be clamped to 1
    BrowseFacetImpl facetWithSmallPage = new BrowseFacetImpl(List.of(), List.of(), 0);
    facetWithSmallPage.installDependencies(mock(EventManager.class));
    facetWithSmallPage.attach(repository);
    facetWithSmallPage.init();
    facetWithSmallPage.start();

    Field browseNodeManagerField = BrowseFacetImpl.class.getDeclaredField("browseNodeManager");
    browseNodeManagerField.setAccessible(true);
    browseNodeManagerField.set(facetWithSmallPage, browseNodeManager);

    Field browseNodeGeneratorField = BrowseFacetImpl.class.getDeclaredField("browseNodeGenerator");
    browseNodeGeneratorField.setAccessible(true);
    browseNodeGeneratorField.set(facetWithSmallPage, browseNodeGenerator);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(1);

    FluentAsset asset = mock(FluentAsset.class);

    Continuation<FluentAsset> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.size()).thenReturn(1);
    doAnswer(invocation -> {
      Consumer<FluentAsset> consumer = invocation.getArgument(0);
      consumer.accept(asset);
      return null;
    }).when(firstPage).forEach(any());
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    Continuation<FluentAsset> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    // Page size should be 1 (clamped from 0)
    when(fluentAssets.browse(eq(1), isNull())).thenReturn(firstPage);
    when(fluentAssets.browse(eq(1), eq("token1"))).thenReturn(emptyPage);

    when(browseNodeGenerator.hasMultipleAssetsPerComponent()).thenReturn(false);
    when(browseNodeGenerator.computeAssetPaths(any())).thenReturn(emptyList());

    facetWithSmallPage.rebuildBrowseNodes(null);

    // Verify browse was called with pageSize 1
    verify(fluentAssets).browse(eq(1), isNull());
  }

  // --- lookupBrowseNodeGenerator fallback to "default" ---

  @SuppressWarnings("unchecked")
  @Test
  public void testDoStart_fallsBackToDefaultBrowseNodeGenerator() throws Exception {
    BrowseNodeGenerator defaultGenerator = mock(BrowseNodeGenerator.class);

    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(Map.of("raw", formatStoreManager), Map.of("default", defaultGenerator));

    BrowseFacetImpl facet = new BrowseFacetImpl(List.of(), List.of(), 1000);
    facet.installDependencies(mock(EventManager.class));

    // Use a format that does not have a specific generator ("maven2" instead of "raw")
    Repository mavenRepo = mock(Repository.class);
    when(mavenRepo.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(mavenRepo.getFormat()).thenReturn(new Format("raw")
    {
    });
    when(mavenRepo.getName()).thenReturn("maven-repo");

    facet.attach(mavenRepo);
    facet.init();
    facet.start();

    // Access the browseNodeGenerator field to verify the default was used
    Field generatorField = BrowseFacetImpl.class.getDeclaredField("browseNodeGenerator");
    generatorField.setAccessible(true);
    Object actualGenerator = generatorField.get(facet);

    // Since "raw" is not in the map (only "default" is), it should fall back to default
    assertThat(actualGenerator, is(defaultGenerator));
  }

  // --- lookupFormatStoreManager failure ---

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void testDoStart_throwsWhenFormatStoreManagerNotFound() throws Exception {
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(
            Collections.emptyMap(), // no format store managers
            Map.of("raw", browseNodeGenerator));

    BrowseFacetImpl facet = new BrowseFacetImpl(List.of(), List.of(), 1000);
    facet.installDependencies(mock(EventManager.class));

    Repository repo = mock(Repository.class);
    when(repo.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repo.getFormat()).thenReturn(new Format("raw")
    {
    });
    when(repo.getName()).thenReturn("test-repo");

    facet.attach(repo);
    facet.init();
    facet.start();
  }

  // --- lookupBrowseNodeGenerator failure ---

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void testDoStart_throwsWhenBrowseNodeGeneratorNotFound() throws Exception {
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(
            Map.of("raw", formatStoreManager),
            Collections.emptyMap()); // no generators at all, not even default

    BrowseFacetImpl facet = new BrowseFacetImpl(List.of(), List.of(), 1000);
    facet.installDependencies(mock(EventManager.class));

    Repository repo = mock(Repository.class);
    when(repo.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repo.getFormat()).thenReturn(new Format("raw")
    {
    });
    when(repo.getName()).thenReturn("test-repo");

    facet.attach(repo);
    facet.init();
    facet.start();
  }

  // --- rebuildBrowseNodes error handling ---

  @SuppressWarnings("unchecked")
  @Test
  public void testRebuildBrowseNodes_handlesExceptionGracefully() {
    when(contentFacet.assets()).thenThrow(new RuntimeException("DB error"));

    underTest.rebuildBrowseNodes(mock(Consumer.class));

    // The method should catch the exception and log it, not propagate
    verify(browseNodeManager).deleteBrowseNodes();
  }

  private static List<BrowseNode> generateParentNodes() {
    BrowseNodeData parentNodeTwo = new BrowseNodeData();
    parentNodeTwo.setAssetCount(0L);
    parentNodeTwo.setNodeId(2L);

    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(0L);
    parentNode.setNodeId(3L);

    return List.of(parentNodeTwo, parentNode);
  }
}
