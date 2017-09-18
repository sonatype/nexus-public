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
package org.sonatype.nexus.repository.browse;

import java.util.List;

import org.sonatype.nexus.repository.browse.AbstractPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractPathBrowseNodeGeneratorTest
{
  private BrowseNodeGenerator generator = new AbstractPathBrowseNodeGenerator()
  {
  };

  @Test
  public void computeAssetPath() {
    Component component = null;
    Asset asset = createAsset("asset/path/foo");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains("asset", "path", "foo"));
  }

  @Test
  public void computeComponentPathWithComponent() {
    Component component = createComponent("component", "group", "1.0.0");
    Asset asset = createAsset("asset/path/foo");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains("asset", "path", "foo"));
  }

  @Test
  public void computeComponentPathNoComponent() {
    Asset asset = createAsset("asset/path/foo");

    List<String> path = generator.computeComponentPath(asset, null);

    assertThat(path.size(), is(0));
  }

  private Asset createAsset(final String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }

  private Component createComponent(final String name, final String group, final String version) {
    Component component = new Component();
    component.name(name);
    component.group(group);
    component.version(version);

    return component;
  }
}
