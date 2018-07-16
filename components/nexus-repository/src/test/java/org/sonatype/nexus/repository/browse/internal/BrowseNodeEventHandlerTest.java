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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.config.internal.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrowseNodeEventHandlerTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "maven2repsoitory";

  private BrowseNodeEventHandler handler;

  @Mock
  private BrowseNodeManager browseNodeManager;

  @Before
  public void setup() {
    handler = new BrowseNodeEventHandler(browseNodeManager);
  }

  @Test
  public void onAssetCreated() {
    Asset asset = new Asset();

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(event.isLocal()).thenReturn(true);

    handler.on(event);

    verify(browseNodeManager).createFromAsset(REPOSITORY_NAME, asset);
  }

  @Test
  public void onAssetCreated_remote() {
    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.isLocal()).thenReturn(false);

    handler.on(event);

    verify(browseNodeManager, never()).createFromAsset(any(), any());
  }

  @Test
  public void onAssetDeleted() {
    EntityId assetId = mock(EntityId.class);

    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    when(event.getAssetId()).thenReturn(assetId);
    when(event.isLocal()).thenReturn(true);

    handler.on(event);

    verify(browseNodeManager).deleteAssetNode(assetId);
  }

  @Test
  public void onAssetDeleted_remote() {
    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    when(event.isLocal()).thenReturn(false);

    handler.on(event);

    verify(browseNodeManager, never()).deleteAssetNode(any());
  }

  @Test
  public void onConfigurationDeleted() {
    ConfigurationDeletedEvent event = mock(ConfigurationDeletedEvent.class);
    when(event.isLocal()).thenReturn(true);
    when(event.getRepositoryName()).thenReturn(REPOSITORY_NAME);

    handler.on(event);

    verify(browseNodeManager).deleteByRepository(REPOSITORY_NAME);
  }

  @Test
  public void onConfigurationDeleted_remote() {
    ConfigurationDeletedEvent event = mock(ConfigurationDeletedEvent.class);
    when(event.isLocal()).thenReturn(false);

    handler.on(event);

    verify(browseNodeManager, never()).deleteByRepository(any());
  }
}
