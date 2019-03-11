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
package org.sonatype.nexus.repository.npm.internal.tasks;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacet;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.AttributeChange;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.npm.internal.tasks.ReindexNpmRepositoryTask;
import org.sonatype.nexus.repository.npm.internal.tasks.ReindexNpmRepositoryTaskDescriptor;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.orientechnologies.orient.core.id.ORID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.sonatype.nexus.repository.RepositoryTaskSupport.ALL_REPOSITORIES;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.PACKAGE_ROOT;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.REPOSITORY_ROOT;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class ReindexNpmRepositoryTaskTest
    extends TestSupport
{
  static final String NPM_V1_SEARCH_UNSUPPORTED = "npm_v1_search_unsupported";

  static final String TASK_ID = "test-id";

  static final String TASK_NAME = "test-task";

  static final String REPOSITORY_NAME = "test-repository";

  static final String ASSET_ID = "asset-id";

  static final String BAD_ASSET_ID = "bad-asset-id";

  static final String TEST_KEY = "name";

  static final String TEST_VALUE = "test-name";

  static final Map<String, Object> FORMAT_ATTRIBUTES = unmodifiableMap(singletonMap(TEST_KEY, TEST_VALUE));

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository repository;

  @Mock
  StorageTx storageTx;

  @Mock
  StorageFacet storageFacet;

  @Mock
  SearchFacet searchFacet;

  @Mock
  AttributesFacet attributesFacet;

  @Mock
  NpmSearchFacet npmSearchFacet;

  @Mock
  NpmPackageParser npmPackageParser;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  ORID assetId;

  @Mock
  Asset asset;

  @Mock
  BlobRef assetBlobRef;

  @Mock
  Blob assetBlob;

  @Mock
  InputStream assetInputStream;

  @Mock
  NestedAttributesMap formatAttributes;

  @Mock
  ORID badAssetId;

  @Mock
  Asset badAsset;

  @Mock
  BlobRef badAssetBlobRef;

  @Mock
  NestedAttributesMap badAssetFormatAttributes;

  @Mock
  NestedAttributesMap changeAttributes;

  ReindexNpmRepositoryTask underTest;

  @Before
  public void setUp() throws Exception {
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.blobRef()).thenReturn(assetBlobRef);

    when(badAsset.formatAttributes()).thenReturn(badAssetFormatAttributes);
    when(badAsset.blobRef()).thenReturn(badAssetBlobRef);

    when(assetEntityAdapter.recordIdentity(asset)).thenReturn(assetId);
    when(assetEntityAdapter.recordIdentity(badAsset)).thenReturn(badAssetId);

    when(assetId.toString()).thenReturn(ASSET_ID);
    when(badAssetId.toString()).thenReturn(BAD_ASSET_ID);

    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(repositoryManager.browse()).thenReturn(singletonList(repository));

    when(repository.facet(SearchFacet.class)).thenReturn(searchFacet);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(NpmSearchFacet.class)).thenReturn(npmSearchFacet);
    when(repository.facet(AttributesFacet.class)).thenReturn(attributesFacet);
    when(repository.optionalFacet(NpmSearchFacet.class)).thenReturn(Optional.of(npmSearchFacet));

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageTx.requireBlob(assetBlobRef)).thenReturn(assetBlob);
    when(storageTx.requireBlob(badAssetBlobRef)).thenThrow(new RuntimeException("bad asset"));
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        emptyList());

    when(assetBlob.getInputStream()).thenReturn(assetInputStream);
    when(npmPackageParser.parsePackageJson(any())).thenReturn(FORMAT_ATTRIBUTES);

    doAnswer(invocation -> {
      AttributeChange attributeChange = (AttributeChange) invocation.getArguments()[0];
      attributeChange.apply(changeAttributes);
      return null;
    }).when(attributesFacet).modifyAttributes(any(AttributeChange.class));

    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setEnabled(true);
    configuration.setName(TASK_NAME);
    configuration.setId(TASK_ID);
    configuration.setTypeId(ReindexNpmRepositoryTaskDescriptor.TYPE_ID);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, ALL_REPOSITORIES);

    underTest = new ReindexNpmRepositoryTask(npmPackageParser, assetEntityAdapter);
    underTest.install(repositoryManager, new GroupType());
    underTest.configure(configuration);
  }

  @Test
  public void repositoryWithoutNpmSearchFacetIsIgnored() throws Exception {
    when(repository.optionalFacet(NpmSearchFacet.class)).thenReturn(Optional.empty());

    underTest.call();

    verifyNoMoreInteractions(searchFacet);
    verifyNoMoreInteractions(attributesFacet);
    verifyNoMoreInteractions(storageTx);
  }

  @Test
  public void searchIndexIsRebuilt() throws Exception {
    underTest.call();

    verify(searchFacet).rebuildIndex();
  }

  @Test
  public void repositoryRootAssetIsIgnored() throws Exception {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(REPOSITORY_ROOT.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(asset), emptyList());

    underTest.call();

    verify(storageTx, never()).saveAsset(asset);
  }

  @Test
  public void packageRootAssetIsIgnored() throws Exception {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGE_ROOT.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(asset), emptyList());

    underTest.call();

    verify(storageTx, never()).saveAsset(asset);
  }

  @Test
  public void tarballAssetWithoutFormatAttributesIsIgnored() throws Exception {
    when(npmPackageParser.parsePackageJson(any())).thenReturn(emptyMap());
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(asset), emptyList());

    underTest.call();

    verify(npmPackageParser).parsePackageJson(any());
    verify(formatAttributes, never()).set(TEST_KEY, TEST_VALUE);
    verify(storageTx, never()).saveAsset(asset);
  }

  @Test
  public void tarballAssetWithFormatAttributesIsProcessed() throws Exception {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(asset), emptyList());

    underTest.call();

    verify(npmPackageParser).parsePackageJson(any());
    verify(formatAttributes).set(TEST_KEY, TEST_VALUE);
    verify(storageTx).saveAsset(asset);
  }

  @Test
  public void exceptionOnBadTarballAssetIsGracefullyHandled() throws Exception {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(badAssetFormatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(badAsset), singletonList(asset), emptyList());

    underTest.call();

    verify(badAssetFormatAttributes, never()).set(TEST_KEY, TEST_VALUE);
    verify(storageTx, never()).saveAsset(badAsset);

    verify(npmPackageParser).parsePackageJson(any());
    verify(formatAttributes).set(TEST_KEY, TEST_VALUE);
    verify(storageTx).saveAsset(asset);
  }

  @Test
  public void repositoryFlagClearedWhenTaskIsComplete() throws Exception {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(badAssetFormatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class))).thenReturn(
        singletonList(badAsset), singletonList(asset), emptyList());

    underTest.call();

    verify(changeAttributes).remove(NPM_V1_SEARCH_UNSUPPORTED);
  }

  @Test
  public void repositoryFlagNotClearedWhenTaskDoesNotComplete() throws Exception {
    when(badAssetFormatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(TARBALL.toString());
    when(storageTx.findAssets(any(String.class), any(Map.class), any(Iterable.class), any(String.class)))
        .thenThrow(new RuntimeException("cannot browse"));

    try {
      underTest.call();
      fail("Exception expected");
    }
    catch (Exception e) {
      // ignored
    }

    verify(changeAttributes, never()).remove(NPM_V1_SEARCH_UNSUPPORTED);
  }
}
