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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNodeCrudStore;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.content.browse.DatastoreBrowseNodeGenerator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatastoreBrowseNodeManagerTest
    extends BrowseTestSupport
{
  private static final String MAVEN_2 = "maven2";

  private static final String REPOSITORY_NAME = "repository";

  @Mock
  private BrowseNodeCrudStore<Asset, Component> browseNodeStore;

  private Map<String, DatastoreBrowseNodeGenerator> pathGenerators;

  @Mock
  private DatastoreBrowseNodeGenerator defaultGenerator;

  @Mock
  private DatastoreBrowseNodeGenerator maven2Generator;

  @Mock
  private Repository repository;

  @Mock
  private Format rawFormat;

  private DatastoreBrowseNodeManager browseNodeManager;

  @Before
  public void setup() {
    when(rawFormat.getValue()).thenReturn("raw");

    when(repository.getName()).thenReturn(REPOSITORY_NAME);

    pathGenerators = new HashMap<>();
    pathGenerators.put(DefaultDatastoreBrowseNodeGenerator.NAME, defaultGenerator);
    pathGenerators.put(MAVEN_2, maven2Generator);

    browseNodeManager = new DatastoreBrowseNodeManager(browseNodeStore, pathGenerators);
  }

  @Test
  public void createFromAssetSavesNodesForAssetWithoutComponent() {
    List<BrowsePath> assetPath = toBrowsePaths(singletonList("asset"));
    Asset asset = createAsset("asset");
    setFormat(repository, "otherFormat");

    when(defaultGenerator.computeAssetPaths(asset, empty())).thenReturn(assetPath);

    browseNodeManager.createFromAsset(repository, asset);

    verify(browseNodeStore)
        .createAssetNode(REPOSITORY_NAME, "otherFormat", toBrowsePaths(singletonList(asset.path())), asset);

    verify(defaultGenerator, times(0)).computeComponentPaths(any(), any());

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetSavesNodesForFormatSpecificAssetWithComponent() {
    List<BrowsePath> assetPath = toBrowsePaths(asList("component", "asset"));

    Component component = createComponent("component", null, null);
    Asset asset = createAsset("asset", component);

    setFormat(repository, MAVEN_2);

    when(maven2Generator.computeAssetPaths(asset, of(component))).thenReturn(assetPath);
    when(maven2Generator.computeComponentPaths(asset, component)).thenReturn(assetPath.subList(0, 1));

    browseNodeManager.createFromAsset(repository, asset);

    verify(browseNodeStore)
        .createComponentNode(REPOSITORY_NAME, MAVEN_2, toBrowsePaths(singletonList(component.name())), component);
    verify(browseNodeStore)
        .createAssetNode(REPOSITORY_NAME, MAVEN_2, toBrowsePaths(asList(component.name(), asset.path())), asset);

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetsSavesNodesWithNoComponents() {
    setFormat(repository, MAVEN_2);

    List<Asset> assets = asList(
        createAsset("assetName1"),
        createAsset("assetName2")
    );
    for (Asset asset : assets) {
      String path = asset.path();
      when(maven2Generator.computeAssetPaths(asset, empty())).thenReturn(toBrowsePaths(singletonList(path)));
    }

    browseNodeManager.createFromAssets(repository, assets);

    for (Asset asset : assets) {
      verify(browseNodeStore)
          .createAssetNode(REPOSITORY_NAME, MAVEN_2, toBrowsePaths(singletonList(asset.path())), asset);
    }

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetsSavesNodesWithComponents() {
    setFormat(repository, MAVEN_2);

    List<Component> components = asList(
        createComponent("componentName1", "componentGroup1", "componentVersion1"),
        createComponent("componentName2", "componentGroup2", "componentVersion2")
    );

    List<Asset> assets = asList(
        createAsset("assetName1", components.get(0)),
        createAsset("assetName2", components.get(1))
    );

    for (int i = 0; i < assets.size(); i++) {
      Asset asset = assets.get(i);
      Component component = components.get(i);

      List<BrowsePath> componentsPaths = componentPaths(component);
      List<BrowsePath> assetPaths = assetPaths(component, asset);

      when(maven2Generator.computeComponentPaths(asset, component)).thenReturn(componentsPaths);
      when(maven2Generator.computeAssetPaths(asset, of(component))).thenReturn(assetPaths);
    }

    browseNodeManager.createFromAssets(repository, assets);

    for (int i = 0; i < assets.size(); i++) {
      Asset asset = assets.get(i);
      Component component = components.get(i);

      List<BrowsePath> componentsPaths = componentPaths(component);
      List<BrowsePath> assetPaths = assetPaths(component, asset);

      verify(browseNodeStore).createComponentNode(REPOSITORY_NAME, MAVEN_2, componentsPaths, component);
      verify(browseNodeStore).createAssetNode(REPOSITORY_NAME, MAVEN_2, assetPaths, asset);
    }

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void maybeCreateFromUpdatedAssetSkipsAssetsWithoutBlobRef() {
    List<BrowsePath> assetPath = toBrowsePaths(singletonList("asset"));
    Asset asset = createAsset("asset");

    when(asset.blob()).thenReturn(empty());
    when(browseNodeStore.assetNodeExists(asset)).thenReturn(false);

    browseNodeManager.maybeCreateFromUpdatedAsset(repository, asset);

    verify(defaultGenerator, times(0)).computeAssetPaths(any(), any());
    verify(defaultGenerator, times(0)).computeComponentPaths(any(), any());

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void maybeCreateFromUpdatedAssetSkipsAssetsWithExistingBrowseNode() {
    List<BrowsePath> assetPath = toBrowsePaths(singletonList("asset"));
    Asset asset = createAsset("asset");

    when(asset.blob()).thenReturn(of(mock(AssetBlob.class)));
    when(browseNodeStore.assetNodeExists(asset)).thenReturn(true);

    when(defaultGenerator.computeAssetPaths(asset, empty())).thenReturn(assetPath);

    browseNodeManager.maybeCreateFromUpdatedAsset(repository, asset);

    verify(browseNodeStore).assetNodeExists(asset);

    verify(defaultGenerator, times(0)).computeAssetPaths(any(), any());
    verify(defaultGenerator, times(0)).computeComponentPaths(any(), any());

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void maybeCreateFromUpdatedAssetCreatesForAssetWithContentAndNoExistingBrowseNode() {
    setFormat(repository, "otherFormat");
    List<BrowsePath> assetPath = toBrowsePaths(singletonList("asset"));
    Asset asset = createAsset("asset");

    when(asset.blob()).thenReturn(of(mock(AssetBlob.class)));
    when(browseNodeStore.assetNodeExists(asset)).thenReturn(false);

    when(defaultGenerator.computeAssetPaths(asset, empty())).thenReturn(assetPath);

    browseNodeManager.maybeCreateFromUpdatedAsset(repository, asset);

    verify(browseNodeStore).assetNodeExists(asset);

    verify(defaultGenerator, times(1)).computeAssetPaths(eq(asset), any());
    verify(defaultGenerator, times(0)).computeComponentPaths(any(), any());

    verify(browseNodeStore)
        .createAssetNode(REPOSITORY_NAME, "otherFormat", toBrowsePaths(singletonList(asset.path())), asset);

    verifyNoMoreInteractions(browseNodeStore);
  }

  private void setFormat(final Repository repository, final String format) {
    Format fmt = mock(Format.class);
    when(fmt.getValue()).thenReturn(format);
    when(repository.getFormat()).thenReturn(fmt);
  }

  private List<BrowsePath> componentPaths(final Component component) {
    return toBrowsePaths(asList(component.namespace(), component.name(), component.version()));
  }

  private List<BrowsePath> assetPaths(final Component component, final Asset asset) {
    return toBrowsePaths(asList(component.namespace(), component.name(), component.version(), asset.path()));
  }
}
