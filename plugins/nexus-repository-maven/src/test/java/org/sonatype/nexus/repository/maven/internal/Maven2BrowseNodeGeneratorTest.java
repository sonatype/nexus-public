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
package org.sonatype.nexus.repository.maven.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.maven.internal.Maven2BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Test;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Maven2BrowseNodeGeneratorTest
    extends TestSupport
{
  private BrowseNodeGenerator generator = new Maven2BrowseNodeGenerator();

  @Test
  public void computeAssetPathForAssetWithoutComponent() {
    Asset asset = createAsset("com/sonatype/example/metadata.xml");

    List<String> path = generator.computeAssetPath(asset, null);

    assertThat(path, contains("com", "sonatype", "example", "metadata.xml"));
  }

  @Test
  public void computeAssetPathForAssetWithComponent() {
    Asset asset = createAsset("com/sonatype/example/1.0.0/example-1.0.0.jar");
    Component component = new Component();

    List<String> path = generator.computeAssetPath(asset, component);

    assertThat(path, contains("com", "sonatype", "example", "1.0.0", "example-1.0.0.jar"));
  }

  @Test
  public void computeComponentPathNameOnly() {
    Asset asset = createAsset("name/name.jar");
    Component component = new Component();
    component.name("name");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.name()));
  }

  @Test
  public void computeComponentPathNoGroup() {
    Asset asset = createAsset("name/1.0.0/name-1.0.0.jar");
    Component component = new Component();
    component.name("name");
    component.version("1.0.0");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.name(), component.version()));
  }

  @Test
  public void computeComponentPath() {
    Asset asset = createAsset("group/name/1.0.0/name-1.0.0.jar");
    Component component = new Component();
    component.group("group");
    component.name("name");
    component.version("1.0.0");

    List<String> path = generator.computeComponentPath(asset, component);

    assertThat(path, contains(component.group(), component.name(), component.version()));
  }


  private Asset createAsset(String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }
}
