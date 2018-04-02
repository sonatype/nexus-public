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
package org.sonatype.nexus.repository.npm.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.npm.internal.NpmBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NpmBrowseNodeGeneratorTest
    extends TestSupport
{
  private BrowseNodeGenerator generator = new NpmBrowseNodeGenerator();

  @Test
  public void computeAssetPathScopedComponent() {
    Asset asset = createAsset("@types/jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<String> assetPath = generator.computeAssetPath(asset, component);

    assertThat(assetPath, contains("@types", "jquery", "jquery-1.0.0.tgz"));
  }

  @Test
  public void computeAssetPathComponent() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<String> assetPath = generator.computeAssetPath(asset, component);

    assertThat(assetPath, contains("jquery", "jquery-1.0.0.tgz"));
  }

  @Test
  public void computeComponentPathReturnsAssetPath() {
    Asset asset = createAsset("jquery/-/jquery-1.0.0.tgz");
    Component component = new DefaultComponent();

    List<String> componentPath = generator.computeComponentPath(asset, component);

    assertThat(componentPath, contains("jquery", "jquery-1.0.0.tgz"));
  }

  private Asset createAsset(String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }
}
