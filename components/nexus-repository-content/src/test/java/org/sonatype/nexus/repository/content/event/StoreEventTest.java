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
package org.sonatype.nexus.repository.content.event;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPreDeleteEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StoreEventTest
    extends TestSupport
{
  private AssetData asset;

  @Before
  public void setUp() throws Exception {
    ComponentData component = new ComponentData();
    component.setComponentId(1);
    asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setComponent(component);
  }

  @Test
  public void shouldHaveMeaningfulToString() {
    assertEquals(
        "AssetAttributesEvent{changes=[AttributeChange{operation=SET, key='key', value=value} ]} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}",
        new AssetAttributesEvent(asset, new AttributeChangeSet(AttributeOperation.SET, "key", "value").getChanges()).toString());
    assertEquals("AssetCreatedEvent{} AssetEvent{asset=" + asset.toString() +"} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetCreatedEvent(asset).toString());
    assertEquals("AssetDeletedEvent{} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetDeletedEvent(asset).toString());
    assertEquals("AssetDownloadedEvent{} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetDownloadedEvent(asset).toString());
    assertEquals("AssetKindEvent{} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetKindEvent(asset).toString());
    assertEquals("AssetPreDeleteEvent{} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetPreDeleteEvent(asset).toString());
    assertEquals("AssetPrePurgeEvent{assetIds=[1, 2]} ContentStoreEvent{contentRepositoryId=0, repository=null}", new AssetPrePurgeEvent(0, new int[]{1, 2}).toString());
    assertEquals("AssetPurgedEvent{assetIds=[1, 2]} ContentStoreEvent{contentRepositoryId=0, repository=null}", new AssetPurgedEvent(0, new int[]{1, 2}).toString());
    assertEquals("AssetUploadedEvent{} AssetEvent{asset=" + asset.toString() + "} ContentStoreEvent{contentRepositoryId=1, repository=null}", new AssetUploadedEvent(asset).toString());
  }
}
