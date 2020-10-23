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
package org.sonatype.nexus.repository.pypi.datastore.internal.browse;

import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PypiBrowseNodeGeneratorTest
{
  PypiBrowseNodeGenerator underTest = new PypiBrowseNodeGenerator();

  @Test
  public void computeIndexPaths() {
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/simple/");
    when(asset.component()).thenReturn(Optional.empty());
    List<BrowsePath> actualPaths = underTest.computeAssetPaths(asset);
    List<BrowsePath> expected = ImmutableList.of(createBrowsePath("simple", "/simple/"));
    Assert.assertEquals(expected, actualPaths);
  }

  @Test
  public void computeIndexPackagePaths() {
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/simple/pyglet/");
    when(asset.component()).thenReturn(Optional.empty());
    List<BrowsePath> actualPaths = underTest.computeAssetPaths(asset);
    List<BrowsePath> expected =
        ImmutableList.of(
            createBrowsePath("simple", "/simple/"),
            createBrowsePath("pyglet", "/simple/pyglet")
        );
    Assert.assertEquals(expected, actualPaths);
  }

  @Test
  public void computePackagePaths() {
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/packages/pyglet/1.5.7/pyglet-1.5.7.zip");
    Component component = mock(Component.class);
    when(component.name()).thenReturn("pyglet");
    when(component.version()).thenReturn("1.5.7");
    when(asset.component()).thenReturn(Optional.of(component));
    List<BrowsePath> actualPaths = underTest.computeAssetPaths(asset);
    List<BrowsePath> expected =
        ImmutableList.of(
            createBrowsePath("pyglet", "/pyglet/"),
            createBrowsePath("1.5.7", "/pyglet/1.5.7/"),
            createBrowsePath("pyglet-1.5.7.zip", "/pyglet/1.5.7/pyglet-1.5.7.zip")
        );
    Assert.assertEquals(expected, actualPaths);
  }

  private BrowsePath createBrowsePath(String displayName, String requestPath) {
    return new BrowsePath(displayName, requestPath);
  }
}
