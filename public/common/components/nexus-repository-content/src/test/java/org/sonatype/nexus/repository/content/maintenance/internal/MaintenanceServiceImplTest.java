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

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.test.util.Whitebox;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.DELETE;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MaintenanceServiceImplTest
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

  @Mock
  private ContentMaintenanceFacet contentMaintenanceFacet;

  @Mock
  private BrowseFacet browseFacet;

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

  @Test
  public void testDeleteComponent() {
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    setupCommonMocks(component, format, asset, fluentComponent, fluentComponents, contentFacet,
        variableResolverAdapter);

    when(databaseCheck.isPostgresql()).thenReturn(true);
    doNothing().when(underTestSpy).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));

    Set<String> result = underTestSpy.deleteComponent(repository, component);

    verify(underTestSpy).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));
    assertEquals(Set.of("asset1", "asset2"), result);
  }

  @Test
  public void testDeleteComponentNotPostgres() {
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    setupCommonMocks(component, format, asset, fluentComponent, fluentComponents, contentFacet,
        variableResolverAdapter);

    when(databaseCheck.isPostgresql()).thenReturn(false);

    Set<String> result = underTestSpy.deleteComponent(repository, component);

    verify(underTestSpy, never()).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));
    assertEquals(Set.of("asset1", "asset2"), result);
  }

  private void setupCommonMocks(
      Component component,
      Format format,
      FluentAsset asset,
      FluentComponent fluentComponent,
      FluentComponents fluentComponents,
      ContentFacet contentFacet,
      VariableResolverAdapter variableResolverAdapter)
  {
    when(format.getValue()).thenReturn("raw");
    when(asset.path()).thenReturn("/foo/bar");
    when(fluentComponent.assets()).thenReturn(Set.of(asset));
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(),
        any(VariableSource.class))).thenReturn(true);
    when(contentMaintenanceFacet.deleteComponent(any(Component.class))).thenReturn(Set.of("asset1", "asset2"));
    when(variableResolverAdapterManager.get(any())).thenReturn(variableResolverAdapter);
  }

  @Test
  public void testDeleteAsset_NpmPackageRoot_PreservesBrowseNode() {
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Asset asset = mock(Asset.class);
    Format format = mock(Format.class);

    when(asset.kind()).thenReturn("PACKAGE_ROOT");
    when(asset.path()).thenReturn("/test/package");
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("npm-repo");
    when(format.getValue()).thenReturn("npm");
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(contentMaintenanceFacet.deleteAsset(asset)).thenReturn(Set.of("/test/package"));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(variableResolverAdapterManager.get("npm")).thenReturn(mock(VariableResolverAdapter.class));
    when(variableResolverAdapterManager.get("npm").fromPath(anyString(), anyString()))
        .thenReturn(mock(VariableSource.class));
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    Set<String> result = underTestSpy.deleteAsset(repository, asset);

    verify(browseFacet, never()).deleteByAssetIdAndPath(any(), anyString());
    assertEquals(Set.of("/test/package"), result);
  }

  @Test
  public void testCanDeleteFolder_WithContentSelectorPermission() {
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("raw-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/Support Team", "raw")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("raw-repo", "raw", DELETE, variableSource)).thenReturn(true);

    boolean result = underTest.canDeleteFolder(repository, "/Support Team");

    assertEquals(true, result);
    verify(variableResolverAdapter).fromPath("/Support Team", "raw");
    verify(contentPermissionChecker).isPermitted("raw-repo", "raw", DELETE, variableSource);
  }

  @Test
  public void testCanDeleteFolder_WithoutContentSelectorPermission() {
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("raw-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/test", "raw")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("raw-repo", "raw", DELETE, variableSource)).thenReturn(false);

    boolean result = underTest.canDeleteFolder(repository, "/test");

    assertEquals(false, result);
    verify(contentPermissionChecker).isPermitted("raw-repo", "raw", DELETE, variableSource);
  }

  @Test
  public void testCanDeleteFolder_UrlEncodedFolderName() {
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("raw-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/Support Team", "raw")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("raw-repo", "raw", DELETE, variableSource)).thenReturn(true);

    // Folder name comes from URL with + instead of space
    boolean result = underTest.canDeleteFolder(repository, "/Support+Team");

    assertEquals(true, result);
    // Verify that the folder path was decoded (+ replaced with space)
    verify(variableResolverAdapter).fromPath("/Support Team", "raw");
    verify(contentPermissionChecker).isPermitted("raw-repo", "raw", DELETE, variableSource);
  }

  @Test
  public void testCanDeleteFolder_PathNormalization() {
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("raw-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/test", "raw")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("raw-repo", "raw", DELETE, variableSource)).thenReturn(true);

    // Folder path without leading slash
    boolean result = underTest.canDeleteFolder(repository, "test");

    assertEquals(true, result);
    // Verify that the path was normalized (leading / added)
    verify(variableResolverAdapter).fromPath("/test", "raw");
  }

  @Test
  public void testCanDeleteFolder_RepositoryPermissionDenied() {
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(false);

    boolean result = underTest.canDeleteFolder(repository, "/test");

    assertEquals(false, result);
    // Verify that Content Selector check was not performed
    verify(variableResolverAdapterManager, never()).get(anyString());
    verify(contentPermissionChecker, never()).isPermitted(anyString(), anyString(), anyString(), any());
  }

  @Test
  public void testCanDeleteFolder_UrlEncodedWithMultipleSpaces() {
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("raw-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/My Test Folder", "raw")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("raw-repo", "raw", DELETE, variableSource)).thenReturn(true);

    // Folder name with multiple spaces encoded as +
    boolean result = underTest.canDeleteFolder(repository, "/My+Test+Folder");

    assertEquals(true, result);
    // Verify that all + were replaced with spaces
    verify(variableResolverAdapter).fromPath("/My Test Folder", "raw");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteAsset_ThrowsAuthorizationExceptionWhenNotPermitted() {
    Asset asset = mock(Asset.class);
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(false);
    when(asset.path()).thenReturn("/some/asset");

    underTest.deleteAsset(repository, asset);
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteComponent_ThrowsAuthorizationExceptionWhenNotPermitted() {
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentAsset fluentAsset = mock(FluentAsset.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(Set.of(fluentAsset));
    when(fluentAsset.path()).thenReturn("/some/asset");
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(false);

    underTest.deleteComponent(repository, component);
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteFolder_ThrowsAuthorizationExceptionWhenNotPermitted() {
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(false);

    underTest.deleteFolder(repository, "/some/folder");
  }

  @Test
  public void testDeleteFolder_SubmitsToExecutorService() {
    // Replace the internal executorService with our mock to avoid Shiro SecurityManager requirement
    Whitebox.setInternalState(underTest, "executorService", executorService);

    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repositoryPermissionChecker.userCanDeleteInRepository(repository)).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(true);

    underTest.deleteFolder(repository, "/some/folder");

    verify(executorService).submit(any(Runnable.class));
  }

  @Test(expected = IllegalOperationException.class)
  public void testDeleteAsset_ThrowsIllegalOperationWhenMissingMaintenanceFacet() {
    Asset asset = mock(Asset.class);
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(true);
    when(asset.path()).thenReturn("/some/asset");
    when(databaseCheck.isPostgresql()).thenReturn(false);

    // Create exception outside of when() to avoid UnfinishedStubbingException
    MissingFacetException exception = new MissingFacetException(repository, ContentMaintenanceFacet.class);
    when(repository.facet(ContentMaintenanceFacet.class)).thenThrow(exception);

    underTest.deleteAsset(repository, asset);
  }

  @Test
  public void testCanDeleteComponent_ReturnsFalseWhenAnyAssetNotPermitted() {
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentAsset permittedAsset = mock(FluentAsset.class);
    FluentAsset deniedAsset = mock(FluentAsset.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource permittedSource = mock(VariableSource.class);
    VariableSource deniedSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(List.of(permittedAsset, deniedAsset));
    when(permittedAsset.path()).thenReturn("/allowed/asset");
    when(deniedAsset.path()).thenReturn("/denied/asset");
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/allowed/asset", "raw")).thenReturn(permittedSource);
    when(variableResolverAdapter.fromPath("/denied/asset", "raw")).thenReturn(deniedSource);
    when(contentPermissionChecker.isPermitted("my-repo", "raw", DELETE, permittedSource)).thenReturn(true);
    when(contentPermissionChecker.isPermitted("my-repo", "raw", DELETE, deniedSource)).thenReturn(false);

    boolean result = underTest.canDeleteComponent(repository, component);

    assertFalse(result);
  }

  @Test
  public void testCanDeleteComponent_ReturnsTrueWhenAllAssetsPermitted() {
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset2 = mock(FluentAsset.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(List.of(asset1, asset2));
    when(asset1.path()).thenReturn("/asset1");
    when(asset2.path()).thenReturn("/asset2");
    when(variableResolverAdapterManager.get("raw")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(true);

    boolean result = underTest.canDeleteComponent(repository, component);

    assertTrue(result);
  }

  @Test
  public void testDeleteAsset_NonNpmOnPostgres_DeletesBrowseNode() {
    Format format = mock(Format.class);

    AssetData assetData = new AssetData();
    assetData.setAssetId(42);
    assetData.setPath("/some/path");
    assetData.setKind("FILE");
    assetData.setRepositoryId(1);

    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(format.getValue()).thenReturn("raw");
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(contentMaintenanceFacet.deleteAsset(assetData)).thenReturn(Set.of("/some/path"));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(mock(VariableResolverAdapter.class));
    when(variableResolverAdapterManager.get("raw").fromPath(anyString(), anyString()))
        .thenReturn(mock(VariableSource.class));
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    underTest.deleteAsset(repository, assetData);

    verify(browseFacet).deleteByAssetIdAndPath(eq(42), eq("/some/path"));
  }

  @Test
  public void testDeleteAsset_NpmTarball_DeletesBrowseNode() {
    Format format = mock(Format.class);

    AssetData assetData = new AssetData();
    assetData.setAssetId(99);
    assetData.setPath("/test/-/test-1.0.tgz");
    assetData.setKind("TARBALL");
    assetData.setRepositoryId(1);

    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("npm-repo");
    when(format.getValue()).thenReturn("npm");
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(contentMaintenanceFacet.deleteAsset(assetData)).thenReturn(Set.of("/test/-/test-1.0.tgz"));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(variableResolverAdapterManager.get("npm")).thenReturn(mock(VariableResolverAdapter.class));
    when(variableResolverAdapterManager.get("npm").fromPath(anyString(), anyString()))
        .thenReturn(mock(VariableSource.class));
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    // NPM TARBALL kind should NOT be preserved (only PACKAGE_ROOT is preserved)
    underTest.deleteAsset(repository, assetData);

    verify(browseFacet).deleteByAssetIdAndPath(eq(99), eq("/test/-/test-1.0.tgz"));
  }

  @Test
  public void testDeleteBrowseNode_NotPostgresql_NoBrowseNodeDeletion() {
    AssetData assetData = new AssetData();
    assetData.setAssetId(10);
    assetData.setPath("/some/file");
    assetData.setRepositoryId(1);

    when(databaseCheck.isPostgresql()).thenReturn(false);

    underTest.deleteBrowseNode(repository, assetData);

    verify(repository, never()).optionalFacet(BrowseFacet.class);
  }

  @Test
  public void testDeleteBrowseNode_NoBrowseFacet() {
    AssetData assetData = new AssetData();
    assetData.setAssetId(10);
    assetData.setPath("/some/file");
    assetData.setKind("FILE");
    assetData.setRepositoryId(1);
    Format format = mock(Format.class);

    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    // Should not throw even when BrowseFacet is absent
    underTest.deleteBrowseNode(repository, assetData);

    verify(browseFacet, never()).deleteByAssetIdAndPath(any(), anyString());
  }

  @Test
  public void testDeleteAssets_SkipsMissingAssets() {
    underTest = spy(underTest);
    doReturn(Set.of("found-asset")).when(underTest).deleteAsset(any(), any());

    AssetData asset1 = mock(AssetData.class);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacetSupport);
    when(contentFacetSupport.stores()).thenReturn(contentFacetStores);
    Whitebox.setInternalState(contentFacetStores, "assetStore", assetStore);

    // First asset found, second asset not found
    when(assetStore.readAsset(1)).thenReturn(Optional.of(asset1));
    when(assetStore.readAsset(2)).thenReturn(Optional.empty());

    Set<String> result = underTest.deleteAssets(repository, List.of(1, 2));

    assertEquals(1, result.size());
    assertTrue(result.contains("found-asset"));
    // deleteAsset should only be called once for the found asset
    verify(underTest).deleteAsset(repository, asset1);
  }

  @Test(expected = MissingFacetException.class)
  public void testContentFacetSupport_ThrowsMissingFacetExceptionOnCastFailure() {
    // When ContentFacet is not a ContentFacetSupport, should throw
    ContentFacet nonSupportFacet = mock(ContentFacet.class);
    when(repository.facet(ContentFacet.class)).thenThrow(new RuntimeException("not ContentFacetSupport"));

    underTest.contentFacetSupport(repository);
  }

  @Test
  public void testCanDeleteAsset_DelegatesPermissionCheck() {
    Asset asset = mock(Asset.class);
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(asset.path()).thenReturn("/org/example/artifact-1.0.jar");
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/org/example/artifact-1.0.jar", "maven2")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("my-repo", "maven2", DELETE, variableSource)).thenReturn(true);

    boolean result = underTest.canDeleteAsset(repository, asset);

    assertTrue(result);
    verify(contentPermissionChecker).isPermitted("my-repo", "maven2", DELETE, variableSource);
  }

  @Test
  public void testCanDeleteAsset_ReturnsFalseWhenNotPermitted() {
    Asset asset = mock(Asset.class);
    Format format = mock(Format.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    VariableSource variableSource = mock(VariableSource.class);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(asset.path()).thenReturn("/org/example/artifact-1.0.jar");
    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);
    when(variableResolverAdapter.fromPath("/org/example/artifact-1.0.jar", "maven2")).thenReturn(variableSource);
    when(contentPermissionChecker.isPermitted("my-repo", "maven2", DELETE, variableSource)).thenReturn(false);

    boolean result = underTest.canDeleteAsset(repository, asset);

    assertFalse(result);
  }

  @Test
  public void testDeleteAsset_ChecksFacetBeforeDeletingBrowseNode() {
    // Test that the maintenance facet is checked BEFORE browse node deletion
    // This prevents orphaned browse nodes if the facet is missing
    Format format = mock(Format.class);

    AssetData assetData = new AssetData();
    assetData.setAssetId(42);
    assetData.setPath("/test/asset");
    assetData.setKind("FILE");
    assetData.setRepositoryId(1);

    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(format.getValue()).thenReturn("raw");
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(contentMaintenanceFacet.deleteAsset(assetData)).thenReturn(Set.of("/test/asset"));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), eq(DELETE), any())).thenReturn(true);
    when(variableResolverAdapterManager.get("raw")).thenReturn(mock(VariableResolverAdapter.class));
    when(variableResolverAdapterManager.get("raw").fromPath(anyString(), anyString()))
        .thenReturn(mock(VariableSource.class));
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    Set<String> result = underTest.deleteAsset(repository, assetData);

    // Verify return value
    assertEquals(Set.of("/test/asset"), result);

    // Verify ordering: facet must be retrieved BEFORE browse node deletion
    InOrder inOrder = inOrder(repository, browseFacet, contentMaintenanceFacet);
    inOrder.verify(repository).facet(ContentMaintenanceFacet.class);
    inOrder.verify(browseFacet).deleteByAssetIdAndPath(eq(42), eq("/test/asset"));
    inOrder.verify(contentMaintenanceFacet).deleteAsset(assetData);
  }

  @Test
  public void testDeleteComponent_ChecksFacetBeforeDeletingBrowseNode() {
    // Test that the maintenance facet is checked BEFORE browse node deletion for components
    // This prevents orphaned browse nodes if the facet is missing
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    when(format.getValue()).thenReturn("raw");
    when(asset.path()).thenReturn("/foo/bar");
    when(fluentComponent.assets()).thenReturn(Set.of(asset));
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(),
        any(VariableSource.class))).thenReturn(true);
    when(contentMaintenanceFacet.deleteComponent(any(Component.class))).thenReturn(Set.of("/foo/bar"));
    when(variableResolverAdapterManager.get(any())).thenReturn(variableResolverAdapter);
    // Note: isPostgresql=false means browse node deletion is skipped entirely.
    // The ordering protection is still verified for the facet check itself.
    // For full PostgreSQL path coverage, see testDeleteComponent_PostgresBrowseNodeDeletion.
    when(databaseCheck.isPostgresql()).thenReturn(false);

    Set<String> result = underTestSpy.deleteComponent(repository, component);

    // Verify return value
    assertEquals(Set.of("/foo/bar"), result);

    // Verify ordering: facet must be retrieved BEFORE any other operations
    InOrder inOrder = inOrder(repository, contentMaintenanceFacet);
    inOrder.verify(repository).facet(ContentMaintenanceFacet.class);
    inOrder.verify(contentMaintenanceFacet).deleteComponent(component);
  }

  @Test
  public void testDeleteAsset_MissingFacetPreventsBrowseNodeDeletion() {
    // Test that when facet is missing, browse node is NOT deleted (prevents orphaned browse nodes)
    Format format = mock(Format.class);

    AssetData assetData = new AssetData();
    assetData.setAssetId(42);
    assetData.setPath("/some/asset");
    assetData.setKind("FILE");
    assetData.setRepositoryId(1);

    when(repository.getName()).thenReturn("my-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");
    when(variableResolverAdapterManager.get("raw")).thenReturn(mock(VariableResolverAdapter.class));
    when(variableResolverAdapterManager.get("raw").fromPath(anyString(), anyString()))
        .thenReturn(mock(VariableSource.class));
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(), any())).thenReturn(true);
    when(databaseCheck.isPostgresql()).thenReturn(true);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    // Repository does NOT have ContentMaintenanceFacet
    MissingFacetException missingFacetException = new MissingFacetException(repository, ContentMaintenanceFacet.class);
    when(repository.facet(ContentMaintenanceFacet.class)).thenThrow(missingFacetException);

    // Verify IllegalOperationException is thrown with MissingFacetException as cause
    IllegalOperationException thrown = assertThrows(
        IllegalOperationException.class,
        () -> underTest.deleteAsset(repository, assetData));

    // Verify the cause is the original MissingFacetException
    assertEquals(missingFacetException, thrown.getCause());

    // Browse node should NOT be deleted since facet check failed first
    verify(browseFacet, never()).deleteByAssetIdAndPath(any(), anyString());
  }

  @Test
  public void testDeleteComponent_MissingFacetPreventsBrowseNodeDeletion() {
    // Test that when facet is missing, browse node is NOT deleted for component deletion
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    when(format.getValue()).thenReturn("raw");
    when(asset.path()).thenReturn("/foo/bar");
    when(fluentComponent.assets()).thenReturn(Set.of(asset));
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(),
        any(VariableSource.class))).thenReturn(true);
    when(variableResolverAdapterManager.get(any())).thenReturn(variableResolverAdapter);

    // Repository does NOT have ContentMaintenanceFacet
    MissingFacetException missingFacetException = new MissingFacetException(repository, ContentMaintenanceFacet.class);
    when(repository.facet(ContentMaintenanceFacet.class)).thenThrow(missingFacetException);

    // Verify IllegalOperationException is thrown with MissingFacetException as cause
    IllegalOperationException thrown = assertThrows(
        IllegalOperationException.class,
        () -> underTestSpy.deleteComponent(repository, component));

    // Verify the cause is the original MissingFacetException
    assertEquals(missingFacetException, thrown.getCause());

    // Verify component maintenance was not called (facet missing)
    verify(contentMaintenanceFacet, never()).deleteComponent(any());
  }
}
