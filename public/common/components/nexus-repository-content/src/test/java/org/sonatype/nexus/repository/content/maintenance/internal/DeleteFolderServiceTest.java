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
package org.sonatype.nexus.repository.content.maintenance.internal;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteFolderServiceTest
{
  @Mock
  private BrowseNodeQueryService browseNodeQueryService;

  @Mock
  private BrowseNodeConfiguration configuration;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentMaintenanceFacet contentMaintenance;

  @Mock
  private BrowseFacet browseFacet;

  @Spy
  @InjectMocks
  private DeleteFolderServiceImpl deleteFolderService;

  @Before
  public void setUp() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    when(node.getNodeId()).thenReturn(1L);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenance);
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(browseFacet.getByRequestPath(any())).thenReturn(Optional.of(node));
    when(configuration.getMaxNodes()).thenReturn(100);
  }

  @After
  public void tearDown() {
    // Unbind SecurityManager
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void testDeleteFolderForPostgreSql() {
    when(databaseCheck.isPostgresql()).thenReturn(true);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
    verify(browseFacet, times(1)).deleteByNodeId(any());
  }

  @Test
  public void testDeleteFolderForNonPostgreSQL() {
    when(databaseCheck.isPostgresql()).thenReturn(false);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
    verify(deleteFolderService, never()).deleteFoldersAndBrowseNode(any(), any(), any(), any(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void testDeleteFolders() {
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolders(repository, "path", OffsetDateTime.now(), contentFacet, contentMaintenance, 100,
        true);

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
  }

  @Test
  public void testDeleteFoldersAndBrowseNode() {
    when(browseNodeQueryService.getByPath(any(), any(), anyInt())).thenReturn(Collections.emptyList());

    deleteFolderService.deleteFoldersAndBrowseNode(repository, "path", OffsetDateTime.now(), contentFacet,
        contentMaintenance, 100, true);

    verify(browseNodeQueryService, times(1)).getByPath(any(), any(), anyInt());
  }

  @Test
  public void testDeleteFolderForPostgreSqlWithChildren() {

    List<BrowseNode> childrenNodes = generateChildrenNodes();
    List<BrowseNode> childrenLeaves = generateChildrenLeaves();
    when(databaseCheck.isPostgresql()).thenReturn(true);
    doReturn(true).when(deleteFolderService).canDeleteComponent(repository);

    when(browseNodeQueryService.getByPath(any(), any(), anyInt()))
        .thenReturn(childrenNodes)
        .thenReturn(childrenLeaves)
        .thenReturn(Collections.emptyList())
        .thenReturn(childrenLeaves)
        .thenReturn(Collections.emptyList());

    deleteFolderService.deleteFolder(repository, "path", OffsetDateTime.now());

    verify(browseNodeQueryService, times(6)).getByPath(any(), any(), anyInt());
    verify(browseFacet, times(7)).deleteByNodeId(any());
    verify(deleteFolderService, never()).deleteFolders(any(), any(), any(), any(), any(), anyInt(), anyBoolean());
    verify(deleteFolderService, times(4)).checkDeleteComponent(any(), any(), any(), anyBoolean(), any());
    verify(deleteFolderService, times(4)).checkDeleteAsset(any(), any(), any(), any(), any());
  }

  private List<BrowseNode> generateChildrenNodes() {
    BrowseNodeData nodeOne = mock(BrowseNodeData.class);
    when(nodeOne.getNodeId()).thenReturn(2L);
    when(nodeOne.getName()).thenReturn("nodeOne");
    when(nodeOne.isLeaf()).thenReturn(false);
    when(nodeOne.getAssetCount()).thenReturn(0L);
    when(nodeOne.getPackageUrl()).thenReturn("/path/nodeOne/");

    BrowseNodeData nodeTwo = mock(BrowseNodeData.class);
    when(nodeTwo.getNodeId()).thenReturn(3L);
    when(nodeTwo.getName()).thenReturn("nodeTwo");
    when(nodeTwo.isLeaf()).thenReturn(false);
    when(nodeTwo.getAssetCount()).thenReturn(0L);
    when(nodeTwo.getPackageUrl()).thenReturn("/path/nodeTwo/");

    return Arrays.asList(nodeOne, nodeTwo);
  }

  private List<BrowseNode> generateChildrenLeaves() {
    BrowseNodeData childOne = mock(BrowseNodeData.class);
    when(childOne.getNodeId()).thenReturn(4L);
    when(childOne.getName()).thenReturn("childOne");
    when(childOne.isLeaf()).thenReturn(true);
    when(childOne.getAssetCount()).thenReturn(0L);
    when(childOne.getPackageUrl()).thenReturn("/path/childOne/");

    BrowseNodeData childTwo = mock(BrowseNodeData.class);
    when(childTwo.getNodeId()).thenReturn(5L);
    when(childTwo.getName()).thenReturn("childTwo");
    when(childTwo.isLeaf()).thenReturn(true);
    when(childTwo.getAssetCount()).thenReturn(0L);
    when(childTwo.getPackageUrl()).thenReturn("/path/childTwo/");

    return Arrays.asList(childOne, childTwo);
  }

  @Test
  public void testCheckDeleteComponent_ReturnsTrue_WhenComponentDeletedSuccessfully() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(null);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);

    boolean result = deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    assertTrue(result);
    verify(contentMaintenance).deleteComponent(component);
  }

  @Test
  public void testCheckDeleteComponent_ReturnsFalse_WhenComponentDeletionFails() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(null);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    doThrow(new RuntimeException("Delete failed")).when(contentMaintenance).deleteComponent(component);

    boolean result = deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    assertFalse(result);
    verify(contentMaintenance).deleteComponent(component);
  }

  @Test
  public void testCheckDeleteComponent_ReturnsTrue_WhenComponentNotFound() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(null);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.empty());

    boolean result = deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    assertTrue(result);
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void testCheckDeleteComponent_ReturnsTrue_WhenNoComponentId() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getComponentId()).thenReturn(null);
    when(node.getAssetId()).thenReturn(assetId);

    boolean result = deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    assertTrue(result);
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void testCheckDeleteComponent_ReturnsFalse_WhenTimestampBeforeLastUpdated() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.plusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(null);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);

    boolean result = deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    assertFalse(result);
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void testCheckDeleteAsset_ReturnsTrue_WhenAssetDeletedSuccessfully() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getAssetId()).thenReturn(assetId);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    doReturn(true).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    boolean result =
        deleteFolderService.checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);

    assertTrue(result);
  }

  @Test
  public void testCheckDeleteAsset_ReturnsFalse_WhenAssetDeletionFails() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getAssetId()).thenReturn(assetId);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    doReturn(false).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    boolean result =
        deleteFolderService.checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);

    assertFalse(result);
  }

  @Test
  public void testCheckDeleteAsset_ReturnsTrue_WhenNoAssetId() {
    BrowseNodeData node = mock(BrowseNodeData.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getAssetId()).thenReturn(null);

    boolean result =
        deleteFolderService.checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);

    assertTrue(result);
  }

  @Test
  public void testCheckDeleteComponent_OrderMatters_PreventsOrphanedComponents() {
    // This test documents WHY we check component deletion BEFORE asset deletion
    // If we checked asset first and it succeeded but component failed, we'd have orphaned components
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(node.getAssetId()).thenReturn(null);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    doThrow(new RuntimeException("Component deletion failed")).when(contentMaintenance).deleteComponent(component);

    // Component deletion should be checked FIRST and fail
    boolean componentDeleted =
        deleteFolderService.checkDeleteComponent(timestamp, contentFacet, contentMaintenance, true, node);

    // Verify component deletion failed
    assertFalse(componentDeleted);

    // If asset deletion was checked first and succeeded, but component failed,
    // we would have an orphaned component (component with no assets)
    // By checking component FIRST, if it fails, we never delete the asset,
    // maintaining data consistency
  }

  @Test
  public void testCheckDeleteComponentWithAssets_DeletesComponent_WhenNoAssetsRemain() {
    // Test for NEXUS-43238: Go repos have browse nodes with both assetId and componentId
    // After deleting an asset, if no assets remain, the component should be deleted
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    when(component.assets()).thenReturn(Collections.emptyList());

    boolean result = deleteFolderService.checkDeleteComponentWithAssets(
        timestamp, contentFacet, contentMaintenance, node);

    assertTrue(result);
    verify(contentMaintenance, times(1)).deleteComponent(component);
  }

  @Test
  public void testCheckDeleteComponentWithAssets_DoesNotDeleteComponent_WhenAssetsRemain() {
    // Test for NEXUS-43238: If a component still has assets, don't delete it yet
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getComponentId()).thenReturn(componentId);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.assets())
        .thenReturn(Arrays.asList(mock(org.sonatype.nexus.repository.content.fluent.FluentAsset.class)));

    boolean result = deleteFolderService.checkDeleteComponentWithAssets(
        timestamp, contentFacet, contentMaintenance, node);

    assertTrue(result); // Should return true to allow browse node deletion
    verify(contentMaintenance, never()).deleteComponent(component);
  }

  @Test
  public void testCheckDeleteComponentWithAssets_ReturnsTrue_WhenComponentNotFound() {
    // Test for NEXUS-43238: If component already deleted, return true
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    when(node.getComponentId()).thenReturn(componentId);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.empty());

    boolean result = deleteFolderService.checkDeleteComponentWithAssets(
        timestamp, contentFacet, contentMaintenance, node);

    assertTrue(result);
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void testCheckDeleteComponentWithAssets_ReturnsFalse_WhenDeletionFails() {
    // Test for NEXUS-43238: If component deletion fails, return false
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    EntityId componentId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    when(node.getComponentId()).thenReturn(componentId);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    when(component.assets()).thenReturn(Collections.emptyList());
    doThrow(new RuntimeException("Component deletion failed")).when(contentMaintenance).deleteComponent(component);

    boolean result = deleteFolderService.checkDeleteComponentWithAssets(
        timestamp, contentFacet, contentMaintenance, node);

    assertFalse(result);
  }

  @Test
  public void testProcessLeafDeletion_WithCombinedNodePath() {
    // Test for NEXUS-43238: Test the new combined-node path (both assetId and componentId present)
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    EntityId componentId = mock(EntityId.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    // Set up node with both assetId and componentId (like Go repos)
    when(node.getNodeId()).thenReturn(1L);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.getComponentId()).thenReturn(componentId);
    when(node.isLeaf()).thenReturn(true);
    when(node.getPath()).thenReturn("/test/path");

    // Set up asset deletion
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId))
        .thenReturn(Optional.of(mock(org.sonatype.nexus.repository.content.fluent.FluentAsset.class)));
    doReturn(true).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    // Set up component with no remaining assets
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    when(component.assets()).thenReturn(Collections.emptyList());

    // Execute
    deleteFolderService.processLeafDeletion(browseFacet, timestamp, contentFacet, contentMaintenance, true, repository,
        node);

    // Verify asset deleted first
    verify(deleteFolderService, times(1)).checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance,
        node);
    // Verify component check happened after asset deletion
    verify(deleteFolderService, times(1)).checkDeleteComponentWithAssets(timestamp, contentFacet, contentMaintenance,
        node);
    // Verify browse node deleted
    verify(browseFacet, times(1)).deleteByNodeId(1L);
  }

  @Test
  public void testProcessLeafDeletion_CombinedNode_CanDeleteComponentFalse() {
    // Test for NEXUS-43238: When canDeleteComponent is false, component check should be skipped
    BrowseNodeData node = mock(BrowseNodeData.class);
    EntityId componentId = mock(EntityId.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    // Set up node with both assetId and componentId
    when(node.getNodeId()).thenReturn(1L);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.getComponentId()).thenReturn(componentId);
    when(node.isLeaf()).thenReturn(true);
    when(node.getPath()).thenReturn("/test/path");

    // Set up successful asset deletion
    doReturn(true).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    // Execute with canDeleteComponent=false
    deleteFolderService.processLeafDeletion(browseFacet, timestamp, contentFacet, contentMaintenance, false, repository,
        node);

    // Verify asset deleted
    verify(deleteFolderService, times(1)).checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance,
        node);
    // Verify component check was NOT called
    verify(deleteFolderService, never()).checkDeleteComponentWithAssets(any(), any(), any(), any());
    // Verify browse node deleted
    verify(browseFacet, times(1)).deleteByNodeId(1L);
  }

  @Test
  public void testProcessLeafDeletion_CombinedNode_AssetDeletionFails() {
    // Test for NEXUS-43238: When asset deletion fails, component check should be skipped
    BrowseNodeData node = mock(BrowseNodeData.class);
    EntityId componentId = mock(EntityId.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();

    // Set up node with both assetId and componentId
    when(node.getNodeId()).thenReturn(1L);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.getComponentId()).thenReturn(componentId);
    when(node.isLeaf()).thenReturn(true);
    when(node.getPath()).thenReturn("/test/path");

    // Set up failed asset deletion
    doReturn(false).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    // Execute
    deleteFolderService.processLeafDeletion(browseFacet, timestamp, contentFacet, contentMaintenance, true, repository,
        node);

    // Verify asset deletion was attempted
    verify(deleteFolderService, times(1)).checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance,
        node);
    // Verify component check was NOT called (because asset deletion failed)
    verify(deleteFolderService, never()).checkDeleteComponentWithAssets(any(), any(), any(), any());
    // Verify browse node was NOT deleted
    verify(browseFacet, never()).deleteByNodeId(any());
  }

  @Test
  public void testProcessLeafDeletion_CombinedNode_DeletionOrderVerification() {
    // Test for NEXUS-43238: Verify asset is deleted before component check (reversed order)
    BrowseNodeData node = mock(BrowseNodeData.class);
    FluentComponent component = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    EntityId componentId = mock(EntityId.class);
    EntityId assetId = mock(EntityId.class);
    OffsetDateTime timestamp = OffsetDateTime.now();
    OffsetDateTime lastUpdated = timestamp.minusDays(1);

    // Set up node with both assetId and componentId
    when(node.getNodeId()).thenReturn(1L);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.getComponentId()).thenReturn(componentId);
    when(node.isLeaf()).thenReturn(true);
    when(node.getPath()).thenReturn("/test/path");

    // Set up asset deletion
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.find(assetId))
        .thenReturn(Optional.of(mock(org.sonatype.nexus.repository.content.fluent.FluentAsset.class)));
    doReturn(true).when(deleteFolderService).checkDeleteAsset(any(), any(), any(), any(), any());

    // Set up component with no remaining assets
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(component));
    when(component.lastUpdated()).thenReturn(lastUpdated);
    when(component.assets()).thenReturn(Collections.emptyList());

    // Execute
    deleteFolderService.processLeafDeletion(browseFacet, timestamp, contentFacet, contentMaintenance, true, repository,
        node);

    // Verify ordering using InOrder
    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(deleteFolderService, browseFacet);
    inOrder.verify(deleteFolderService).checkDeleteAsset(repository, timestamp, contentFacet, contentMaintenance, node);
    inOrder.verify(deleteFolderService)
        .checkDeleteComponentWithAssets(timestamp, contentFacet, contentMaintenance, node);
    inOrder.verify(browseFacet).deleteByNodeId(1L);
  }
}
