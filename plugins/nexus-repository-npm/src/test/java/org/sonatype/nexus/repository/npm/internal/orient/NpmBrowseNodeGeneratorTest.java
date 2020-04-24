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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.util.List;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static java.util.Arrays.asList;

public class NpmBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private BrowseNodeGenerator generator = new NpmBrowseNodeGenerator();

  @Test
  public void computeAssetPathScopedComponent() {
    Asset asset = createAsset("@types/jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeAssetPaths(asset, component);

    assertPaths(asList("@types", "jquery", "jquery-1.0.0.tgz"), asList("@types", "jquery", "-/jquery-1.0.0.tgz"), assetPaths);
  }

  @Test
  public void computeAssetPathComponent() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeAssetPaths(asset, component);

    assertPaths(asList("jquery", "jquery-1.0.0.tgz"), asList("jquery", "-/jquery-1.0.0.tgz"), assetPaths);
  }

  @Test
  public void computeComponentPathReturnsAssetPath() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<BrowsePaths> assetPaths = generator.computeComponentPaths(asset, component);

    assertPaths(asList("jquery", "jquery-1.0.0.tgz"), asList("jquery", "-/jquery-1.0.0.tgz"), assetPaths);
  }
}
