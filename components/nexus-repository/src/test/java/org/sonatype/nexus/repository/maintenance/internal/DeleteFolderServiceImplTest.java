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
package org.sonatype.nexus.repository.maintenance.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetStore;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.base.Suppliers;
import org.apache.shiro.authz.UnauthorizedException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DeleteFolderServiceImplTest
    extends TestSupport
{
  @Mock
  BrowseNodeStore browseNodeStore;

  @Mock
  BrowseNodeConfiguration configuration;

  @Mock
  AssetStore assetStore;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  ComponentMaintenance componentMaintenance;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  VariableSource variableSource;

  @Mock
  private SecurityHelper securityHelper;

  Collection<BrowseNode> browseNodes = new ArrayList<>();

  DeleteFolderServiceImpl service;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ComponentMaintenance.class)).thenReturn(componentMaintenance);
    when(repository.getName()).thenReturn("repo");
    when(repository.getFormat()).thenReturn(new Format("maven2") { });
    when(configuration.getMaxNodes()).thenReturn(1);
    when(browseNodeStore.getByPath(repository, Arrays.asList("com", "sonatype"), 1, null)).thenReturn(browseNodes);
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromAsset(any(Asset.class))).thenReturn(variableSource);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(Suppliers.ofInstance(storageTx));
    when(securityHelper.isPermitted(new RepositoryViewPermission(repository, BreadActions.DELETE)))
        .thenReturn(new boolean[]{false});

    service = new DeleteFolderServiceImpl(browseNodeStore, configuration, assetStore, contentPermissionChecker,
        variableResolverAdapterManager, securityHelper);
  }

  @Test
  public void deleteFolderShouldDeleteAllAssetsWithinPath() {
    mockAssetComponentBrowseNode("assetId", "componentId", DateTime.now().minusHours(1));
    when(contentPermissionChecker.isPermitted(repository.getName(), "maven2", BreadActions.DELETE, variableSource))
        .thenReturn(true);

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verify(componentMaintenance).deleteAsset(new DetachedEntityId("assetId"));
  }

  @Test
  public void deleteFolderShouldSkipAssetCreatedAfterTheGivenTimestamp() {
    mockAssetBrowseNode("assetId", DateTime.now().plusHours(1));

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verifyZeroInteractions(componentMaintenance);
  }

  @Test
  public void deleteFolderShouldSkipAssetThatUserHasNoPrivilegeToDelete() {
    mockAssetBrowseNode("assetId", DateTime.now().minusHours(1));
    when(componentMaintenance.deleteAsset(new DetachedEntityId("assetId"))).thenThrow(new UnauthorizedException());
    when(contentPermissionChecker.isPermitted(repository.getName(), "maven2", BreadActions.DELETE, variableSource))
        .thenReturn(false);

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verifyNoMoreInteractions(componentMaintenance);
  }

  @Test
  public void deleteFolderShouldDeleteDanglingComponentNodes() {
    mockComponentBrowseNode("componentId", DateTime.now().minusHours(1));
    when(securityHelper.isPermitted(new RepositoryViewPermission(repository, BreadActions.DELETE)))
        .thenReturn(new boolean[]{true});

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verify(componentMaintenance).deleteComponent(new DetachedEntityId("componentId"));
  }

  @Test
  public void deleteFolderShouldSkipComponentCreatedAfterTheGivenTimestamp() {
    mockComponentBrowseNode("componentId", DateTime.now().plusHours(1));
    when(securityHelper.isPermitted(new RepositoryViewPermission(repository, BreadActions.DELETE)))
        .thenReturn(new boolean[]{true});

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verifyNoMoreInteractions(componentMaintenance);
  }

  @Test
  public void deleteFolderShouldSkipComponentThatUserHasNoPrivilegeToDelete() {
    mockComponentBrowseNode("componentId", DateTime.now().minusHours(1));

    service.deleteFolder(repository, "com/sonatype", DateTime.now(), () -> false);

    verifyNoMoreInteractions(componentMaintenance);
  }

  private BrowseNode mockAssetBrowseNode(final String assetId, final DateTime blobCreated) {
    Asset asset = new Asset();
    asset.blobCreated(blobCreated);

    DetachedEntityId assetEntityId = new DetachedEntityId(assetId);

    BrowseNode browseNode = new BrowseNode();
    browseNode.setLeaf(true);
    browseNode.setAssetId(assetEntityId);

    when(assetStore.getById(assetEntityId)).thenReturn(asset);

    browseNodes.add(browseNode);

    return browseNode;
  }

  private BrowseNode mockComponentBrowseNode(final String componentId, final DateTime lastUpdated) {
    DetachedEntityId entityId = new DetachedEntityId(componentId);
    BrowseNode browseNode = new BrowseNode();
    browseNode.setLeaf(true);
    browseNode.setComponentId(entityId);

    Component component = mock(Component.class);
    when(component.lastUpdated()).thenReturn(lastUpdated);
    when(storageTx.findComponent(entityId)).thenReturn(component);

    browseNodes.add(browseNode);

    return browseNode;
  }

  private BrowseNode mockAssetComponentBrowseNode(final String assetId, final String componentId, final DateTime blobCreated) {
    Asset asset = new Asset();
    asset.blobCreated(blobCreated);
    Component component = new DefaultComponent();

    DetachedEntityId assetEntityId = new DetachedEntityId(assetId);
    DetachedEntityId componentEntityId = new DetachedEntityId(componentId);

    BrowseNode browseNode = new BrowseNode();
    browseNode.setLeaf(true);
    browseNode.setAssetId(assetEntityId);
    browseNode.setComponentId(componentEntityId);

    when(assetStore.getById(assetEntityId)).thenReturn(asset);
    when(storageTx.browseAssets(component)).thenReturn(Collections.singletonList(asset));
    browseNodes.add(browseNode);

    return browseNode;
  }
}
