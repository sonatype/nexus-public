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
package org.sonatype.nexus.repository.browse.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentStore;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BrowseNodeManagerTest
    extends TestSupport
{
  private static final String DEFAULT = "default";

  private static final String MAVEN_2 = "maven2";

  private static final String REPOSITORY_NAME = "repository";

  private BrowseNodeManager manager;

  @Mock
  private BrowseNodeStore browseNodeStore;

  @Mock
  private ComponentStore componentStore;

  @Mock
  private BrowseNodeGenerator maven2BrowseNodeGenerator;

  @Mock
  private DefaultBrowseNodeGenerator defaultBrowseNodeGenerator;

  @Mock
  private Repository repository;

  @Before
  public void setup() {
    Map<String, BrowseNodeGenerator> generators = new HashMap<>();
    generators.put(DEFAULT, defaultBrowseNodeGenerator);
    generators.put(MAVEN_2, maven2BrowseNodeGenerator);

    manager = new BrowseNodeManager(browseNodeStore, componentStore, generators);

    when(repository.getName()).thenReturn(REPOSITORY_NAME);
  }

  @Test
  public void createFromAssetSavesNodesForAssetWithoutComponent() {
    List<String> assetPath = asList("asset");
    Component component = null;
    Asset asset = createAsset("asset", "assetId", "otherFormat", null);

    when(defaultBrowseNodeGenerator.computeAssetPath(asset, component)).thenReturn(assetPath);
    when(defaultBrowseNodeGenerator.computeComponentPath(asset, component)).thenReturn(emptyList());

    manager.createFromAsset(REPOSITORY_NAME, asset);

    verify(browseNodeStore).createAssetNode(REPOSITORY_NAME, asList(asset.name()), asset);

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetSavesNodesForFormatSpecificAssetWithComponent() {
    List<String> assetPath = asList("component", "asset");
    Component component = createComponent("component", null, null, "componentId");
    Asset asset = createAsset("asset", "assetId", MAVEN_2, EntityHelper.id(component));

    when(maven2BrowseNodeGenerator.computeAssetPath(asset, component)).thenReturn(assetPath);
    when(maven2BrowseNodeGenerator.computeComponentPath(asset, component)).thenReturn(assetPath.subList(0, 1));

    manager.createFromAsset(REPOSITORY_NAME, asset);

    verify(browseNodeStore).createComponentNode(REPOSITORY_NAME, asList(component.name()), component);
    verify(browseNodeStore).createAssetNode(REPOSITORY_NAME, asList(component.name(), asset.name()), asset);

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetsSavesNodesWithNoComponents() {
    List<Asset> assets = asList(
        createAsset("assetName1", "assetId1", MAVEN_2, null),
        createAsset("assetName2", "assetId2", MAVEN_2, null)
    );

    for (Asset asset : assets) {
      String name = asset.name();
      when(maven2BrowseNodeGenerator.computeAssetPath(asset, null)).thenReturn(asList(name));
    }

    manager.createFromAssets(repository, assets);

    for (Asset asset : assets) {
      verify(browseNodeStore).createAssetNode(REPOSITORY_NAME, asList(asset.name()), asset);
    }

    verifyNoMoreInteractions(browseNodeStore);
  }

  @Test
  public void createFromAssetsSavesNodesWithComponents() {
    List<Component> components = asList(
        createComponent("componentName1", "componentGroup1", "componentVersion1", "componentId1"),
        createComponent("componentName2", "componentGroup2", "componentVersion2", "componentId2")
    );

    List<Asset> assets = asList(
        createAsset("assetName1", "assetId1", MAVEN_2, EntityHelper.id(components.get(0))),
        createAsset("assetName2", "assetId2", MAVEN_2, EntityHelper.id(components.get(1)))
    );

    for (int i = 0; i < assets.size(); i++) {
      Asset asset = assets.get(i);
      Component component = components.get(i);
      String name = asset.name();
      when(maven2BrowseNodeGenerator.computeAssetPath(asset, component)).thenReturn(
          asList(component.group(), component.name(), component.version(), name));
      when(maven2BrowseNodeGenerator.computeComponentPath(asset, component)).thenReturn(asList(
          component.group(), component.name(), component.version()));
    }

    manager.createFromAssets(repository, assets);

    for (int i = 0; i < assets.size(); i++) {
      Asset asset = assets.get(i);
      Component component = components.get(i);
      verify(browseNodeStore).createComponentNode(REPOSITORY_NAME,
          asList(component.group(), component.name(), component.version()), component);
      verify(browseNodeStore).createAssetNode(REPOSITORY_NAME,
          asList(component.group(), component.name(), component.version(), asset.name()), asset);
    }

    verifyNoMoreInteractions(browseNodeStore);
  }


  private Asset createAsset(final String assetName, final String assetId, final String format, final EntityId componentId) {
    EntityMetadata entityMetadata = mock(EntityMetadata.class);
    when(entityMetadata.getId()).thenReturn(new DetachedEntityId(assetId));

    Asset asset = mock(Asset.class);
    when(asset.getEntityMetadata()).thenReturn(entityMetadata);
    when(asset.name()).thenReturn(assetName);
    when(asset.format()).thenReturn(format);

    if (componentId != null) {
      when(asset.componentId()).thenReturn(componentId);
    }

    Format fmt = mock(Format.class);
    when(fmt.getValue()).thenReturn(format);
    when(repository.getFormat()).thenReturn(fmt);

    return asset;
  }

  private Component createComponent(final String name,
                                    final String group,
                                    final String version,
                                    final String componentId)
  {
    EntityMetadata metadata = mock(EntityMetadata.class);
    when(metadata.getId()).thenReturn(new DetachedEntityId(componentId));

    Component component = new DefaultComponent();
    component.name(name);
    component.group(group);
    component.version(version);
    component.setEntityMetadata(metadata);

    when(componentStore.read(EntityHelper.id(component))).thenReturn(component);

    return component;
  }
}
