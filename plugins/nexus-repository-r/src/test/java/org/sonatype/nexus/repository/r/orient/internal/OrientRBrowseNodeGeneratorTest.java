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
package org.sonatype.nexus.repository.r.orient.internal;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.r.orient.internal.OrientRBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Test;

public class OrientRBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private OrientRBrowseNodeGenerator generator = new OrientRBrowseNodeGenerator();

  @Test
  public void shouldComputeAssetAndComponentPath() {
    final String commonPath = "bin/macosx/el-capitan/contrib/3.6";
    final String lastSegment = "devtools_2.2.1.tgz";
    final String assetPath = commonPath + "/" + lastSegment;

    final String componentName = "devtools";
    final String componentVersion = "2.2.1";

    final String componentBrowsePath = commonPath + "/" + componentName + "/" + componentVersion;
    final String assetBrowsePath = componentBrowsePath + "/" + lastSegment;

    Component component = createComponent(componentName, commonPath, componentVersion);
    Asset asset = createAsset(assetPath);

    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, component);
    List<BrowsePaths> pathsComponent = generator.computeComponentPaths(asset, component);

    assertPaths(Arrays.asList(assetBrowsePath.split("/")), pathsAsset);
    assertPaths(Arrays.asList(componentBrowsePath.split("/")), pathsComponent, true);
  }

  @Test
  public void shouldComputeAssetAndComponentPathWithoutNameDuplication() {
    final String commonPath = "src/contrib/Archive/ggplot2";
    final String lastSegment = "ggplot2_0.9.0.tar.gz";
    final String assetPath = commonPath + "/" + lastSegment;

    final String componentName = "ggplot2";
    final String componentVersion = "0.9.0";

    // Browse path should not have name duplicates
    final String componentBrowsePath = commonPath + "/" + componentVersion;
    final String assetBrowsePath = componentBrowsePath + "/" + lastSegment;

    Component component = createComponent(componentName, commonPath, componentVersion);
    Asset asset = createAsset(assetPath);

    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, component);
    List<BrowsePaths> pathsComponent = generator.computeComponentPaths(asset, component);

    assertPaths(Arrays.asList(assetBrowsePath.split("/")), pathsAsset);
    assertPaths(Arrays.asList(componentBrowsePath.split("/")), pathsComponent, true);
  }

  @Test
  public void shouldComputeAssetPathWithoutComponent() {
    final String assetPath = "bin/macosx/el-capitan/contrib/3.6/devtools_2.2.1.tgz";

    Asset asset = createAsset(assetPath);
    List<BrowsePaths> pathsAsset = generator.computeAssetPaths(asset, null);
    assertPaths(Arrays.asList(assetPath.split("/")), pathsAsset, false);
  }
}
