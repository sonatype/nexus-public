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
package org.sonatype.nexus.repository.content.event.asset;

import org.sonatype.nexus.repository.content.store.AssetData;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssetPathChangedEventTest
{
  @Test
  public void testEventConstruction() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(100);
    asset.setPath("/new/path/artifact.jar");

    String oldPath = "/old/path/artifact.jar";
    String newPath = "/new/path/artifact.jar";

    AssetPathChangedEvent event = new AssetPathChangedEvent(asset, oldPath, newPath);

    assertEquals(asset, event.getAsset());
    assertEquals(oldPath, event.getOldPath());
    assertEquals(newPath, event.getNewPath());
  }

  @Test
  public void testEventExtendsAssetUpdatedEvent() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(100);
    asset.setPath("/new/path");

    AssetPathChangedEvent event = new AssetPathChangedEvent(asset, "/old", "/new");

    assertTrue(event instanceof AssetUpdatedEvent);
  }

  @Test
  public void testToString() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(100);
    asset.setPath("/new/path");

    AssetPathChangedEvent event = new AssetPathChangedEvent(asset, "/old", "/new");

    String str = event.toString();
    assertNotNull(str);
    assertTrue(str.contains("AssetPathChangedEvent"));
    assertTrue(str.contains("/old"));
    assertTrue(str.contains("/new"));
  }

  @Test
  public void testEventWithNullPaths() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(100);
    asset.setPath("/path");

    AssetPathChangedEvent event = new AssetPathChangedEvent(asset, null, "/path");

    assertEquals(null, event.getOldPath());
    assertEquals("/path", event.getNewPath());
  }
}
