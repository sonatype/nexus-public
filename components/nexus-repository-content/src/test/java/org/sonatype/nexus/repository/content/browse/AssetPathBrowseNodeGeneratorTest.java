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
package org.sonatype.nexus.repository.content.browse;

import java.util.List;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;

public class AssetPathBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private AssetPathBrowseNodeGenerator generator;

  @Before
  public void setup() {
    generator = new AssetPathBrowseNodeGenerator()
    {
    };
  }

  @Test
  public void computeAssetPathsNoComponent() {
    Asset asset = createAsset("asset/path/foo");

    List<BrowsePath> paths = generator.computeAssetPaths(asset);

    assertPaths(asList("asset", "path", "foo"), paths);
  }

  @Test
  public void computeAssetPathsWithComponent() {
    Component component = createComponent("component", "group", "1.0.0");
    Asset asset = createAsset("asset/path/foo", component);

    List<BrowsePath> paths = generator.computeAssetPaths(asset);

    assertPaths(asList("asset", "path", "foo"), paths);
  }

  @Test
  public void computeComponentPathsWithComponent() {
    Component component = createComponent("component", "group", "1.0.0");
    Asset asset = createAsset("asset/path/foo", component);

    List<BrowsePath> paths = generator.computeComponentPaths(asset);

    assertPaths(asList("asset", "path", "foo"), paths);
  }
}
