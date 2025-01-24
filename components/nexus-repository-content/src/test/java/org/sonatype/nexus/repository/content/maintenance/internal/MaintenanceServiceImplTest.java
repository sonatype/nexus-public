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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.test.util.Whitebox;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MaintenanceServiceImplTest
    extends TestSupport
{
  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private DeleteFolderService deleteFolderService;

  @Mock
  private ExecutorService executorService;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private ContentFacetStores contentFacetStores;

  @Mock
  private AssetStore assetStore;

  private MaintenanceServiceImpl underTest;

  @Before
  public void setUp() {
    underTest = new MaintenanceServiceImpl(contentPermissionChecker, variableResolverAdapterManager,
        repositoryPermissionChecker, deleteFolderService, executorService, databaseCheck);
  }

  @Test
  public void test_MaintenanceServiceImpl_deleteAssets() {
    underTest = spy(underTest);
    doReturn(Set.of("asset1", "asset2")).when(underTest).deleteAsset(any(), any());

    AssetData asset1 = mock(AssetData.class);
    asset1.setAssetId(1);
    asset1.setPath("asset1");
    AssetData asset2 = mock(AssetData.class);
    asset2.setAssetId(2);
    asset2.setPath("asset2");

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacetSupport);
    when(contentFacetSupport.stores()).thenReturn(contentFacetStores);
    Whitebox.setInternalState(contentFacetStores, "assetStore", assetStore);
    when(assetStore.readAsset(anyInt()))
        .thenReturn(Optional.of(asset1))
        .thenReturn(Optional.of(asset2));

    Set<String> result = underTest.deleteAssets(repository, List.of(1, 2));

    assertEquals(2, result.size());
  }

  @Test
  public void test_MaintenanceServiceImpl_deleteAssets_EmptyAssets() {
    underTest = spy(underTest);
    doReturn(Set.of()).when(underTest).deleteAsset(any(), any());

    Set<String> result = underTest.deleteAssets(repository, List.of());

    assertEquals(0, result.size());
  }
}
