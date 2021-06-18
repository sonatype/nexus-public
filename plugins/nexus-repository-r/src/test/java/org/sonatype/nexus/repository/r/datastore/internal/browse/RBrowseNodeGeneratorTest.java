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
package org.sonatype.nexus.repository.r.datastore.internal.browse;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;

import org.junit.Test;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH;

/**
 * Unit tests for {@link RBrowseNodeGenerator}
 */
public class RBrowseNodeGeneratorTest
{
  private final RBrowseNodeGenerator generator = new RBrowseNodeGenerator();

  @Test
  public void shouldComputeAssetAndComponentPath() {
    final String commonPath = "bin/macosx/el-capitan/contrib/3.6";
    final String lastSegment = "devtools_2.2.1.tgz";
    final String assetPath = commonPath + SLASH + lastSegment;

    final String componentName = "devtools";
    final String componentVersion = "2.2.1";

    final String componentBrowsePath = commonPath + SLASH + componentName + SLASH + componentVersion;
    final String assetBrowsePath = componentBrowsePath + SLASH + lastSegment;

    Component component = createComponent(componentName, commonPath, componentVersion);
    Asset asset = createAsset(assetPath, component);

    List<BrowsePath> pathsAsset = generator.computeAssetPaths(asset);
    List<BrowsePath> pathsComponent = generator.computeComponentPaths(asset);

    assertPaths(Arrays.asList(assetBrowsePath.split(SLASH)), pathsAsset);
    assertPaths(Arrays.asList(componentBrowsePath.split(SLASH)), pathsComponent, true);
  }

  @Test
  public void shouldComputeAssetAndComponentPathWithoutNameDuplication() {
    final String commonPath = "src/contrib/Archive/ggplot2";
    final String lastSegment = "ggplot2_0.9.0.tar.gz";
    final String assetPath = commonPath + SLASH + lastSegment;

    final String componentName = "ggplot2";
    final String componentVersion = "0.9.0";

    // Browse path should not have name duplicates
    final String componentBrowsePath = commonPath + SLASH + componentVersion;
    final String assetBrowsePath = componentBrowsePath + SLASH + lastSegment;

    Component component = createComponent(componentName, commonPath, componentVersion);
    Asset asset = createAsset(assetPath, component);

    List<BrowsePath> pathsAsset = generator.computeAssetPaths(asset);
    List<BrowsePath> pathsComponent = generator.computeComponentPaths(asset);

    assertPaths(Arrays.asList(assetBrowsePath.split(SLASH)), pathsAsset);
    assertPaths(Arrays.asList(componentBrowsePath.split(SLASH)), pathsComponent, true);
  }

  @Test
  public void shouldComputeAssetPathWithoutComponent() {
    final String assetPath = "bin/macosx/el-capitan/contrib/3.6/devtools_2.2.1.tgz";

    Asset asset = createAsset(assetPath);
    List<BrowsePath> pathsAsset = generator.computeAssetPaths(asset);
    assertPaths(Arrays.asList(assetPath.split(SLASH)), pathsAsset, false);
  }

  private Asset createAsset(final String path) {
    return createAsset(path, null);
  }

  private Asset createAsset(final String path, final Component component) {
    Asset asset = mock(Asset.class);

    when(asset.blob()).thenReturn(of(mock(AssetBlob.class)));
    when(asset.component()).thenReturn(ofNullable(component));
    when(asset.path()).thenReturn(path);

    return asset;
  }

  private Component createComponent(final String name, final String namespace, final String version) {
    Component component = mock(Component.class);

    when(component.name()).thenReturn(name);
    when(component.namespace()).thenReturn(namespace);
    when(component.version()).thenReturn(version);

    return component;
  }

  protected void assertPaths(List<String> expectedPaths, List<BrowsePath> paths) {
    assertPaths(expectedPaths, paths, false);
  }

  protected void assertPaths(List<String> expectedPaths, List<BrowsePath> paths, boolean withTrailingSlash) {
    assertPaths(expectedPaths, expectedPaths, paths, withTrailingSlash);
  }

  protected void assertPaths(
      List<String> expectedBrowsePaths,
      List<String> expectedRequestPaths,
      List<BrowsePath> paths,
      boolean withTrailingSlash)
  {
    assertThat(expectedRequestPaths.size(), is(expectedBrowsePaths.size()));
    assertThat(paths.size(), is(expectedBrowsePaths.size()));

    String requestPath = SLASH;

    for (int i = 0; i < expectedBrowsePaths.size(); i++) {
      requestPath += expectedRequestPaths.get(i);
      if (withTrailingSlash || i < expectedBrowsePaths.size() - 1) {
        requestPath += SLASH;
      }
      assertThat(paths.get(i).getDisplayName(), is(expectedBrowsePaths.get(i)));
      assertThat(paths.get(i).getRequestPath(), is(requestPath));
    }
  }
}
