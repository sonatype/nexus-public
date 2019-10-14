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
package org.sonatype.nexus.repository.cocoapods.internal;

import java.util.List;

import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Test;

import static java.util.Arrays.asList;

/**
 * @since 3.19
 */
public class CocoapodsBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private CocoapodsBrowseNodeGenerator generator = new CocoapodsBrowseNodeGenerator();

  @Test
  public void shouldComputeAssetAndComponentPath() {
    final String assetName = "assetName";
    final String assetPath = "path/to/" + assetName;
    final String componentGroup = "testGroup";
    final String componentName = "testComponentName";
    final String componentVersion = "testVersion";

    Component component = createComponent(componentName, componentGroup, componentVersion);
    Asset asset = createAsset(assetPath);

    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, component);
    List<BrowsePaths> pathsComponent = generator.computeComponentPaths(asset, component);

    assertPaths(asList("pods", componentGroup, componentName, componentVersion, assetName), pathsAsset, false);
    assertPaths(asList("pods", componentGroup, componentName, componentVersion), pathsComponent, true);
  }

  @Test
  public void shouldRemoveRestParametersFromAssetName() {
    final String assetName = "assetName?param=value";
    final String assetViewName = "assetName";
    final String assetPath = "path/to/" + assetName;
    final String componentGroup = "testGroup";
    final String componentName = "testComponentName";
    final String componentVersion = "testVersion";

    Component component = createComponent(componentName, componentGroup, componentVersion);
    Asset asset = createAsset(assetPath);

    List<BrowsePaths> pathsAssetWithComponent = generator.computeAssetPaths(asset, component);
    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, component);

    assertPaths(asList("pods", componentGroup, componentName, componentVersion, assetViewName), pathsAsset, false);
    assertPaths(asList("pods", componentGroup, componentName, componentVersion, assetViewName), pathsAssetWithComponent, false);
  }

  @Test
  public void shouldComputeAssetPathWithoutComponent() {
    final String assetPath = "path/to/assetName";
    Asset asset = createAsset(assetPath);

    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, null);
    assertPaths(asList("path", "to", "assetName"), pathsAsset, false);
  }
}
