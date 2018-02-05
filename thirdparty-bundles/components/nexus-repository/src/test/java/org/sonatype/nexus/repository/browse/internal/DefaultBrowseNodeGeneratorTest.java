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
package org.sonatype.nexus.repository.browse.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultBrowseNodeGeneratorTest
    extends TestSupport
{
  private BrowseNodeGenerator generator = new DefaultBrowseNodeGenerator();

  @Test
  public void computeAssetPathWithNoComponent() {
    Component component = null;
    Asset asset = createAsset("/path/asset");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains("path", "asset"));
  }

  @Test
  public void computeAssetPathWithNoComponent_trailingSlash() {
    Component component = null;
    Asset asset = createAsset("/path/asset/");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains("path", "asset"));
  }

  @Test
  public void computeAssetPathWithComponentNameOnly() {
    Component component = createComponent("component", null, null);
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains(component.name(), "assetName"));
  }

  @Test
  public void computeAssetPathWithComponentNoGroup() {
    Component component = createComponent("component", null, "1.0.0");
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains(component.name(), component.version(), "assetName"));
  }

  @Test
  public void computeAssetPathWithComponent() {
    Component component = createComponent("component", "group", "1.0.0");
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains(component.group(), component.name(), component.version(), "assetName"));
  }

  @Test
  public void computeComponentPathWithComponentNameOnly() {
    Component component = createComponent("component", null, null);
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.name()));
  }

  @Test
  public void computeComponentPathWithComponentNoGroup() {
    Component component = createComponent("component", null, "1.0.0");
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.name(), component.version()));
  }

  @Test
  public void computeComponentPathWithComponent() {
    Component component = createComponent("component", "group", "1.0.0");
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.group(), component.name(), component.version()));
  }

  @Test
  public void lastSegmentBehaviour() {
    assertThat(generator.lastSegment(""), is(""));
    assertThat(generator.lastSegment("/"), is(""));
    assertThat(generator.lastSegment("//"), is(""));
    assertThat(generator.lastSegment("///"), is(""));
    assertThat(generator.lastSegment("////"), is(""));
    assertThat(generator.lastSegment("a"), is("a"));
    assertThat(generator.lastSegment("a/"), is("a"));
    assertThat(generator.lastSegment("/a"), is("a"));
    assertThat(generator.lastSegment("/a/"), is("a"));
    assertThat(generator.lastSegment("//a"), is("a"));
    assertThat(generator.lastSegment("a//"), is("a"));
    assertThat(generator.lastSegment("//a//"), is("a"));
    assertThat(generator.lastSegment("a/b"), is("b"));
    assertThat(generator.lastSegment("a/b/"), is("b"));
    assertThat(generator.lastSegment("/a/b"), is("b"));
    assertThat(generator.lastSegment("/a/b/"), is("b"));
    assertThat(generator.lastSegment("a/.b"), is(".b"));
    assertThat(generator.lastSegment("a/b.c"), is("b.c"));
  }

  private Asset createAsset(final String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }

  private Component createComponent(final String name,
                                    final String group,
                                    final String version)
  {
    Component component = new DefaultComponent();
    component.name(name);
    component.group(group);
    component.version(version);

    return component;
  }
}
