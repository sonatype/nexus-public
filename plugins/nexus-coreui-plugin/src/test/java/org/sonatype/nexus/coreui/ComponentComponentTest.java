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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.base.Suppliers;
import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ComponentComponent}.
 */
public class ComponentComponentTest
    extends TestSupport
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository repository;

  @Mock
  ComponentMaintenance componentMaintenance;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  private ComponentComponent underTest;

  @Before
  public void setup() {
    underTest = new ComponentComponent();
    underTest.setRepositoryManager(repositoryManager);
    underTest.setContentPermissionChecker(contentPermissionChecker);
    underTest.setVariableResolverAdapterManager(variableResolverAdapterManager);

    when(repositoryManager.get("testRepositoryName")).thenReturn(repository);
    when(repository.getName()).thenReturn("testRepositoryName");
    when(repository.getFormat()).thenReturn(new Format("testFormat") { });
    when(repository.facet(ComponentMaintenance.class)).thenReturn(componentMaintenance);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(variableResolverAdapterManager.get("testFormat")).thenReturn(variableResolverAdapter);
    when(storageFacet.txSupplier()).thenReturn(Suppliers.ofInstance(storageTx));
  }

  @Test
  public void testDeleteComponent_success() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Collections.singletonList(asset));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    underTest.deleteComponent("testComponentId", "testRepositoryName");
  }

  @Test
  public void testDeleteComponent_success_multipleAssets() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Asset asset2 = mock(Asset.class);
    VariableSource variableSource2 = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(variableResolverAdapter.fromAsset(asset2)).thenReturn(variableSource2);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Arrays.asList(asset, asset2));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource2))
        .thenReturn(true);
    underTest.deleteComponent("testComponentId", "testRepositoryName");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteComponent_failure() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Collections.singletonList(asset));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(false);
    underTest.deleteComponent("testComponentId", "testRepositoryName");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteComponent_failure_multipleAssets() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Asset asset2 = mock(Asset.class);
    VariableSource variableSource2 = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(variableResolverAdapter.fromAsset(asset2)).thenReturn(variableSource2);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Arrays.asList(asset, asset2));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource2))
        .thenReturn(false);
    underTest.deleteComponent("testComponentId", "testRepositoryName");
  }

  @Test
  public void testDeleteAsset_success() {
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Bucket bucket = mock(Bucket.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findAsset(new DetachedEntityId("testAssetId"), bucket)).thenReturn(asset);
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    underTest.deleteAsset("testAssetId", "testRepositoryName");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteAsset_failure() {
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Bucket bucket = mock(Bucket.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findAsset(new DetachedEntityId("testAssetId"), bucket)).thenReturn(asset);
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(false);
    underTest.deleteAsset("testAssetId", "testRepositoryName");
  }
}
