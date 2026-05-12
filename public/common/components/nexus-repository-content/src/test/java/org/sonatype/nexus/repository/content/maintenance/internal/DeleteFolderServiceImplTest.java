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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.security.SecurityHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.DELETE;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DeleteFolderServiceImplTest
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
  private FluentComponents fluentComponents;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private BrowseFacet browseFacet;

  @Mock
  private Format format;

  @Mock
  private VariableResolverAdapter variableResolverAdapter;

  private DeleteFolderServiceImpl underTest;

  @Before
  public void setUp() {
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(repository.getName()).thenReturn("test-repo");

    underTest = new DeleteFolderServiceImpl(
        browseNodeQueryService, configuration, contentPermissionChecker,
        variableResolverAdapterManager, securityHelper, databaseCheck);
  }

  @Test
  public void testConstruction() {
    assertThat(underTest, is(notNullValue()));
  }

  @Test
  public void testCanDeleteComponentDelegatesToSecurityHelper() {
    when(securityHelper.isPermitted(any(RepositoryViewPermission.class)))
        .thenReturn(new boolean[]{true});

    assertThat(underTest.canDeleteComponent(repository), is(true));
  }

  @Test
  public void testCanDeleteComponentReturnsFalseWhenDenied() {
    when(securityHelper.isPermitted(any(RepositoryViewPermission.class)))
        .thenReturn(new boolean[]{false});

    assertThat(underTest.canDeleteComponent(repository), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void testNullBrowseNodeQueryServiceRejected() {
    new DeleteFolderServiceImpl(
        null, configuration, contentPermissionChecker,
        variableResolverAdapterManager, securityHelper, databaseCheck);
  }

  @Test(expected = NullPointerException.class)
  public void testNullConfigurationRejected() {
    new DeleteFolderServiceImpl(
        browseNodeQueryService, null, contentPermissionChecker,
        variableResolverAdapterManager, securityHelper, databaseCheck);
  }

  @Test(expected = NullPointerException.class)
  public void testNullSecurityHelperRejected() {
    new DeleteFolderServiceImpl(
        browseNodeQueryService, configuration, contentPermissionChecker,
        variableResolverAdapterManager, null, databaseCheck);
  }

  @Test(expected = NullPointerException.class)
  public void testNullContentPermissionCheckerRejected() {
    new DeleteFolderServiceImpl(
        browseNodeQueryService, configuration, null,
        variableResolverAdapterManager, securityHelper, databaseCheck);
  }

  @Test(expected = NullPointerException.class)
  public void testNullVariableResolverAdapterManagerRejected() {
    new DeleteFolderServiceImpl(
        browseNodeQueryService, configuration, contentPermissionChecker,
        null, securityHelper, databaseCheck);
  }

  @Test(expected = NullPointerException.class)
  public void testNullDatabaseCheckRejected() {
    new DeleteFolderServiceImpl(
        browseNodeQueryService, configuration, contentPermissionChecker,
        variableResolverAdapterManager, securityHelper, null);
  }

  // ---- deleteFolder: routing based on database type ----

  @Test
  public void deleteFolder_routesToPostgresqlPath_whenPostgresql() {
    DeleteFolderServiceImpl spied = spy(underTest);
    doReturn(true).when(spied).canDeleteComponent(repository);
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenance);
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(configuration.getMaxNodes()).thenReturn(10000);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());
    when(browseFacet.getByRequestPath(anyString())).thenReturn(Optional.empty());

    spied.deleteFolder(repository, "some/path", OffsetDateTime.now());

    verify(repository).facet(BrowseFacet.class);
  }

  @Test
  public void deleteFolder_routesToH2Path_whenNotPostgresql() {
    DeleteFolderServiceImpl spied = spy(underTest);
    doReturn(true).when(spied).canDeleteComponent(repository);
    when(databaseCheck.isPostgresql()).thenReturn(false);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenance);
    when(configuration.getMaxNodes()).thenReturn(10000);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    spied.deleteFolder(repository, "some/path", OffsetDateTime.now());

    verify(repository, never()).facet(BrowseFacet.class);
  }

  // ---- checkDeleteAsset: real delegation through deleteAsset ----

  @Test
  public void checkDeleteAsset_deletesAsset_whenPermittedAndOlderThanTimestamp() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("file.jar", assetId, null);

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(eq("test-repo"), eq("maven2"), eq(DELETE), any()))
        .thenReturn(true);

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance).deleteAsset(fluentAsset);
  }

  @Test
  public void checkDeleteAsset_returnsFalse_whenAssetIsNewerThanTimestamp() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("file.jar", assetId, null);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().plusDays(1));
    when(contentPermissionChecker.isPermitted(eq("test-repo"), eq("maven2"), eq(DELETE), any()))
        .thenReturn(true);

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(false));
    verify(contentMaintenance, never()).deleteAsset(any());
  }

  @Test
  public void checkDeleteAsset_returnsFalse_whenNotPermitted() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("file.jar", assetId, null);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(eq("test-repo"), eq("maven2"), eq(DELETE), any()))
        .thenReturn(false);

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(false));
    verify(contentMaintenance, never()).deleteAsset(any());
  }

  @Test
  public void checkDeleteAsset_returnsTrue_whenNoAssetId() {
    BrowseNode node = mockLeafNode("node", null, null);

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteAsset(any());
  }

  @Test
  public void checkDeleteAsset_returnsTrue_whenAssetNotFound() {
    when(contentFacet.assets()).thenReturn(fluentAssets);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", assetId, null);
    when(fluentAssets.find(assetId)).thenReturn(Optional.empty());

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteAsset(any());
  }

  @Test
  public void checkDeleteAsset_returnsFalse_whenDeleteThrowsException() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("file.jar", assetId, null);

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(eq("test-repo"), eq("maven2"), eq(DELETE), any()))
        .thenReturn(true);
    when(contentMaintenance.deleteAsset(fluentAsset)).thenThrow(new RuntimeException("delete failed"));

    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(false));
  }

  @Test
  public void checkDeleteAsset_usesCurrentTime_whenBlobIsAbsent() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("file.jar", assetId, null);

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.blob()).thenReturn(Optional.empty());
    when(fluentAsset.path()).thenReturn("/some/path");
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(fluentAsset));
    when(contentPermissionChecker.isPermitted(eq("test-repo"), eq("maven2"), eq(DELETE), any()))
        .thenReturn(true);

    // Timestamp in the future -- with no blob, the fallback is now(), so this should be after now()
    boolean result = underTest.checkDeleteAsset(
        repository, OffsetDateTime.now().plusDays(1), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance).deleteAsset(fluentAsset);
  }

  // ---- checkDeleteComponent ----

  @Test
  public void checkDeleteComponent_deletesComponent_whenCanDeleteAndNoAssetOnNode() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, true, node);

    assertThat(result, is(true));
    verify(contentMaintenance).deleteComponent(fluentComponent);
  }

  @Test
  public void checkDeleteComponent_returnsTrueWithoutDeleting_whenCannotDeleteComponent() {
    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, false, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponent_returnsTrueWithoutDeleting_whenNodeHasAssetId() {
    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", assetId, componentId);

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, true, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponent_returnsFalse_whenComponentIsNewerThanTimestamp() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().plusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, true, node);

    assertThat(result, is(false));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponent_returnsTrue_whenComponentNotFound() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);
    when(fluentComponents.find(componentId)).thenReturn(Optional.empty());

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, true, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponent_returnsFalse_whenDeleteThrowsException() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    doThrow(new RuntimeException("delete failed")).when(contentMaintenance).deleteComponent(fluentComponent);

    boolean result = underTest.checkDeleteComponent(
        OffsetDateTime.now(), contentFacet, contentMaintenance, true, node);

    assertThat(result, is(false));
  }

  // ---- checkDeleteComponentWithAssets ----

  @Test
  public void checkDeleteComponentWithAssets_deletesComponent_whenNoAssetsRemain() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponent.assets()).thenReturn(Collections.emptySet());
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance).deleteComponent(fluentComponent);
  }

  @Test
  public void checkDeleteComponentWithAssets_doesNotDeleteComponent_whenAssetsRemain() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.assets()).thenReturn(Set.of(mock(FluentAsset.class)));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponentWithAssets_returnsTrue_whenNoComponentId() {
    BrowseNode node = mockLeafNode("node", null, null);

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verifyNoInteractions(fluentComponents);
  }

  @Test
  public void checkDeleteComponentWithAssets_returnsTrue_whenComponentNotFound() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);
    when(fluentComponents.find(componentId)).thenReturn(Optional.empty());

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(true));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void checkDeleteComponentWithAssets_returnsFalse_whenDeleteFails() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponent.assets()).thenReturn(Collections.emptySet());
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    doThrow(new RuntimeException("delete failed")).when(contentMaintenance).deleteComponent(fluentComponent);

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(false));
  }

  @Test
  public void checkDeleteComponentWithAssets_returnsFalse_whenComponentIsNewerThanTimestamp() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode node = mockLeafNode("node", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().plusDays(1));
    when(fluentComponent.assets()).thenReturn(Collections.emptySet());
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    boolean result = underTest.checkDeleteComponentWithAssets(
        OffsetDateTime.now(), contentFacet, contentMaintenance, node);

    assertThat(result, is(false));
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  // ---- deleteFolders (H2 path) ----

  @Test
  public void deleteFolders_traversesNonLeafNodesAndDeletesLeaves() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    BrowseNode folderNode = mockFolderNode("subfolder");
    EntityId assetId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("file.txt", assetId, null);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(folderNode))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance).deleteAsset(fluentAsset);
  }

  @Test
  public void deleteFolders_combinedNode_deletesAssetFirstThenComponentIfNoAssetsRemain() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("file.jar", assetId, componentId);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponent.assets()).thenReturn(Collections.emptySet());
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance).deleteAsset(fluentAsset);
    verify(contentMaintenance).deleteComponent(fluentComponent);
  }

  @Test
  public void deleteFolders_combinedNode_skipsComponentDeletion_whenCanDeleteComponentIsFalse() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("file.jar", assetId, componentId);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, false);

    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void deleteFolders_combinedNode_skipsComponentDeletion_whenAssetDeleteFails() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("file.jar", assetId, componentId);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(contentMaintenance.deleteAsset(fluentAsset)).thenThrow(new RuntimeException("asset delete failed"));

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void deleteFolders_combinedNode_doesNotDeleteComponent_whenAssetsRemain() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("file.jar", assetId, componentId);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.assets()).thenReturn(Set.of(mock(FluentAsset.class)));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance, never()).deleteComponent(any());
  }

  @Test
  public void deleteFolders_separateNode_deletesComponentOnly_whenOnlyComponentPresent() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("component-node", null, componentId);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance).deleteComponent(fluentComponent);
  }

  @Test
  public void deleteFolders_separateNode_deletesAssetOnly_whenOnlyAssetPresent() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNode leafNode = mockLeafNode("asset-only", assetId, null);

    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(leafNode))
        .thenReturn(Collections.emptyList());

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance).deleteAsset(fluentAsset);
  }

  @Test
  public void deleteFolders_handlesEmptyNodes() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    underTest.deleteFolders(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(contentMaintenance, never()).deleteAsset(any());
    verify(contentMaintenance, never()).deleteComponent(any());
  }

  // ---- processLeafDeletion ----

  @Test
  public void processLeafDeletion_deletesBrowseNode_whenBothAssetAndComponentSucceed() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, componentId);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponent.assets()).thenReturn(Collections.emptySet());
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet).deleteByNodeId(42L);
  }

  @Test
  public void processLeafDeletion_doesNotDeleteBrowseNode_whenAssetDeletionFails() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, componentId);

    FluentAsset fluentAsset = mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(contentMaintenance.deleteAsset(fluentAsset)).thenThrow(new RuntimeException("delete failed"));

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet, never()).deleteByNodeId(any());
  }

  @Test
  public void processLeafDeletion_doesNotDeleteBrowseNode_whenComponentDeletionFails() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));
    doThrow(new RuntimeException("delete failed")).when(contentMaintenance).deleteComponent(fluentComponent);

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet, never()).deleteByNodeId(any());
  }

  @Test
  public void processLeafDeletion_deletesBrowseNode_whenOnlyAssetPresent() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, null);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet).deleteByNodeId(42L);
  }

  @Test
  public void processLeafDeletion_deletesBrowseNode_whenOnlyComponentPresent() {
    when(contentFacet.components()).thenReturn(fluentComponents);

    EntityId componentId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", null, componentId);

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.lastUpdated()).thenReturn(OffsetDateTime.now().minusDays(1));
    when(fluentComponents.find(componentId)).thenReturn(Optional.of(fluentComponent));

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet).deleteByNodeId(42L);
  }

  @Test
  public void processLeafDeletion_handlesExceptionDuringBrowseNodeDeletion() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, null);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    doThrow(new RuntimeException("browse node delete failed")).when(browseFacet).deleteByNodeId(42L);

    // Should not throw -- exception is caught and logged
    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet).deleteByNodeId(42L);
  }

  @Test
  public void processLeafDeletion_doesNotDeleteBrowseNode_whenAssetNotPermitted() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, null);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(false);

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, true, repository, nodeData);

    verify(browseFacet, never()).deleteByNodeId(any());
  }

  @Test
  public void processLeafDeletion_combinedNode_skipsComponentCheck_whenCanDeleteComponentFalse() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    EntityId assetId = mock(EntityId.class);
    EntityId componentId = mock(EntityId.class);
    BrowseNodeData nodeData = mockBrowseNodeData(42L, "/test/path", assetId, componentId);

    mockFluentAssetWithBlob(assetId, OffsetDateTime.now().minusDays(1));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);

    underTest.processLeafDeletion(
        browseFacet, OffsetDateTime.now(), contentFacet, contentMaintenance, false, repository, nodeData);

    // Component should not be checked since canDeleteComponent is false
    verify(fluentComponents, never()).find(any());
    // Browse node should still be deleted since asset was deleted
    verify(browseFacet).deleteByNodeId(42L);
  }

  // ---- deleteFoldersAndBrowseNode (PostgreSQL path) ----

  @Test
  public void deleteFoldersAndBrowseNode_deletesEmptyFolderBrowseNode() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    BrowseNodeData folderBrowseNode = mockBrowseNodeData(99L, "/root/", null, null);
    when(browseFacet.getByRequestPath("/root/")).thenReturn(Optional.of(folderBrowseNode));
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);

    underTest.deleteFoldersAndBrowseNode(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet).deleteByNodeId(99L);
  }

  @Test
  public void deleteFoldersAndBrowseNode_doesNotDeleteFolderBrowseNode_whenNotFound() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    when(browseFacet.getByRequestPath("/root/")).thenReturn(Optional.empty());
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);

    underTest.deleteFoldersAndBrowseNode(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet, never()).deleteByNodeId(any());
  }

  @Test
  public void deleteFoldersAndBrowseNode_handlesExceptionDuringFolderBrowseNodeDeletion() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    BrowseNodeData folderBrowseNode = mockBrowseNodeData(99L, "/root/", null, null);
    when(browseFacet.getByRequestPath("/root/")).thenReturn(Optional.of(folderBrowseNode));
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    doThrow(new RuntimeException("delete failed")).when(browseFacet).deleteByNodeId(99L);

    // Should not throw -- exception is caught and logged
    underTest.deleteFoldersAndBrowseNode(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);
  }

  @Test
  public void deleteFoldersAndBrowseNode_skipsAlreadyProcessedFolders() {
    BrowseNode folderChild = mockFolderNode("child");

    // First call: root has children
    // Second call: child folder is empty
    // Third call: root revisited, still has children -> skip because already processed
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(List.of(folderChild))
        .thenReturn(Collections.emptyList())
        .thenReturn(List.of(folderChild));

    BrowseNodeData childBrowseNode = mockBrowseNodeData(50L, "/root/child/", null, null);
    when(browseFacet.getByRequestPath("/root/child/")).thenReturn(Optional.of(childBrowseNode));
    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);

    underTest.deleteFoldersAndBrowseNode(
        repository, "root", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet).deleteByNodeId(50L);
  }

  // ---- transformTreePathToRequestPath (tested indirectly) ----

  @Test
  public void deleteFoldersAndBrowseNode_addsLeadingSlash_whenMissing() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(browseFacet.getByRequestPath("/noLeadingSlash/")).thenReturn(Optional.empty());

    underTest.deleteFoldersAndBrowseNode(
        repository, "noLeadingSlash", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet).getByRequestPath("/noLeadingSlash/");
  }

  @Test
  public void deleteFoldersAndBrowseNode_addsTrailingSlash_whenMissing() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(browseFacet.getByRequestPath("/hasLeadingSlash/")).thenReturn(Optional.empty());

    underTest.deleteFoldersAndBrowseNode(
        repository, "/hasLeadingSlash", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet).getByRequestPath("/hasLeadingSlash/");
  }

  @Test
  public void deleteFoldersAndBrowseNode_preservesExistingSlashes() {
    when(browseNodeQueryService.getByPath(any(), anyList(), anyInt()))
        .thenReturn(Collections.emptyList());

    when(repository.facet(BrowseFacet.class)).thenReturn(browseFacet);
    when(browseFacet.getByRequestPath("/already/correct/")).thenReturn(Optional.empty());

    underTest.deleteFoldersAndBrowseNode(
        repository, "/already/correct/", OffsetDateTime.now(), contentFacet, contentMaintenance, 10000, true);

    verify(browseFacet).getByRequestPath("/already/correct/");
  }

  // ---- Helper methods ----

  private BrowseNode mockLeafNode(final String name, final EntityId assetId, final EntityId componentId) {
    BrowseNode node = mock(BrowseNode.class);
    when(node.isLeaf()).thenReturn(true);
    when(node.getName()).thenReturn(name);
    when(node.getAssetId()).thenReturn(assetId);
    when(node.getComponentId()).thenReturn(componentId);
    return node;
  }

  private BrowseNode mockFolderNode(final String name) {
    BrowseNode node = mock(BrowseNode.class);
    when(node.isLeaf()).thenReturn(false);
    when(node.getName()).thenReturn(name);
    return node;
  }

  private FluentAsset mockFluentAssetWithBlob(final EntityId assetId, final OffsetDateTime blobCreated) {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    when(assetBlob.blobCreated()).thenReturn(blobCreated);
    when(fluentAsset.blob()).thenReturn(Optional.of(assetBlob));
    when(fluentAsset.path()).thenReturn("/some/path");
    when(fluentAssets.find(assetId)).thenReturn(Optional.of(fluentAsset));
    return fluentAsset;
  }

  private BrowseNodeData mockBrowseNodeData(
      final long nodeId,
      final String path,
      final EntityId assetId,
      final EntityId componentId)
  {
    BrowseNodeData data = mock(BrowseNodeData.class);
    when(data.getNodeId()).thenReturn(nodeId);
    when(data.getPath()).thenReturn(path);
    when(data.getAssetId()).thenReturn(assetId);
    when(data.getComponentId()).thenReturn(componentId);
    when(data.isLeaf()).thenReturn(true);
    return data;
  }
}
