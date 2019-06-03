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
package org.sonatype.nexus.repository.apt.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 3.next
 */
public class AptBrowseNodeGeneratorTest
    extends TestSupport
{
  private AptBrowseNodeGenerator generator = new AptBrowseNodeGenerator();

  @Test
  public void computeComponentPath() {
    Component component = createComponent("nano", "amd64", "1.0.0");
    Asset asset = createAsset("path/assetName");

    List<String> path = generator.computeComponentPath(asset, component);
    assertEquals("packages/n/nano/1.0.0/amd64/nano", String.join("/", path));
  }

  private Asset createAsset(final String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }

  private Component createComponent(final String name, final String group, final String version)
  {
    Component component = new DefaultComponent();
    component.name(name);
    component.group(group);
    component.version(version);

    return component;
  }
}
