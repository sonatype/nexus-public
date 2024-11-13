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
package org.sonatype.nexus.repository.apt.datastore.internal;

import java.util.List;

import org.sonatype.nexus.repository.apt.datastore.internal.browse.AptBrowseNodeGenerator;
import org.sonatype.nexus.repository.content.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

/**
 * @since 3.31
 */
public class AptBrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private AptBrowseNodeGenerator generator = new AptBrowseNodeGenerator();

  @Test
  public void computeComponentPath() {
    ComponentData componentData = new ComponentData();
    componentData.setRepositoryId(1);
    componentData.setComponentId(1);
    componentData.setName("nano");
    componentData.setNamespace("amd64");
    componentData.setVersion("1.0.0");

    AssetData asset = new AssetData();
    asset.setComponent(componentData);
    asset.setPath("/path/assetName");

    List<BrowsePath> paths = generator.computeComponentPaths(asset);

    assertThat(paths.size(), is(6));

    assertThat(paths, containsInAnyOrder(
        new BrowsePath("packages", "/packages/"),
        new BrowsePath("n", "/packages/n/"),
        new BrowsePath("nano", "/packages/n/nano/"),
        new BrowsePath("1.0.0", "/packages/n/nano/1.0.0/"),
        new BrowsePath("amd64", "/packages/n/nano/1.0.0/amd64/"),
        new BrowsePath("nano", "/packages/n/nano/1.0.0/amd64/nano/")));
  }


  @Test
  public void computeAssetPathMetadata() {
    AssetData asset = new AssetData();
    asset.setPath("/path/assetName");

    List<BrowsePath> paths = generator.computeAssetPaths(asset);

    assertThat(paths.size(), is(3));

    assertThat(paths, containsInAnyOrder(
        new BrowsePath("metadata", "/metadata/"),
        new BrowsePath("path", "/metadata/path/"),
        new BrowsePath("assetName", "/metadata/path/assetName/")));
  }

  @Test
  public void computeAssetPathDeb() {
    AssetData asset = new AssetData();
    asset.setPath("/path/assetName.deb");

    List<BrowsePath> paths = generator.computeAssetPaths(asset);

    assertThat(paths.size(), is(3));

    assertThat(paths, containsInAnyOrder(
        new BrowsePath("packages", "/packages/"),
        new BrowsePath("path", "/packages/path/"),
        new BrowsePath("assetName.deb", "/packages/path/assetName.deb/")));
  }

  @Test
  public void computeAssetPathSnapshots() {
    AssetData asset = new AssetData();
    asset.setPath("/snapshots/path/assetName");

    List<BrowsePath> paths = generator.computeAssetPaths(asset);

    assertThat(paths.size(), is(3));

    assertThat(paths, containsInAnyOrder(
        new BrowsePath("snapshots", "/snapshots/"),
        new BrowsePath("path", "/snapshots/path/"),
        new BrowsePath("assetName", "/snapshots/path/assetName/")));
  }
}
