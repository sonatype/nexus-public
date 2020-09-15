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
package org.sonatype.repository.conan.internal.ui;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;
import org.sonatype.repository.conan.internal.orient.ui.ConanBrowseNodeGenerator;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConanBrowseNodeGeneratorTest
    extends TestSupport
{
  Component component;

  ConanBrowseNodeGenerator underTest = new ConanBrowseNodeGenerator();

  @Before
  public void setUp() {
    component = new DefaultComponent().group("vthiery")
        .name("jsonformoderncpp")
        .version("2.1.1");
  }

  @Test
  public void canComputePackagePath() {
    Asset asset = createAsset(
        "conans/jsonformoderncpp/2.1.1/vthiery/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/download_urls");

    List<BrowsePaths> assetPath = underTest.computeAssetPaths(asset, component);

    assertThat(assetPath.size(), is(6));
    assertThat(assetPath.get(0).getDisplayName(), is("vthiery"));
    assertThat(assetPath.get(1).getDisplayName(), is("jsonformoderncpp"));
    assertThat(assetPath.get(2).getDisplayName(), is("2.1.1"));
    assertThat(assetPath.get(3).getDisplayName(), is("packages"));
    assertThat(assetPath.get(4).getDisplayName(), is("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9"));
    assertThat(assetPath.get(5).getDisplayName(), is("download_urls"));
  }

  @Test
  public void canComputePackageName() {
    Asset asset = createAsset(
        "conans/jsonformoderncpp/2.1.1/vthiery/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9");

    List<BrowsePaths> assetPath = underTest.computeAssetPaths(asset, component);

    assertThat(assetPath.size(), is(5));
    assertThat(assetPath.get(0).getDisplayName(), is("vthiery"));
    assertThat(assetPath.get(1).getDisplayName(), is("jsonformoderncpp"));
    assertThat(assetPath.get(2).getDisplayName(), is("2.1.1"));
    assertThat(assetPath.get(3).getDisplayName(), is("packages"));
    assertThat(assetPath.get(4).getDisplayName(), is("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9"));
  }

  @Test
  public void canComputeNonPackagePath() {
    Asset asset = createAsset(
        "conans/jsonformoderncpp/2.1.1/vthiery/stable/download_urls");

    List<BrowsePaths> assetPath = underTest.computeAssetPaths(asset, component);

    assertThat(assetPath.size(), is(4));
    assertThat(assetPath.get(0).getDisplayName(), is("vthiery"));
    assertThat(assetPath.get(1).getDisplayName(), is("jsonformoderncpp"));
    assertThat(assetPath.get(2).getDisplayName(), is("2.1.1"));
    assertThat(assetPath.get(3).getDisplayName(), is("download_urls"));
  }

  private Asset createAsset(String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }
}
