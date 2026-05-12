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
package org.sonatype.nexus.cleanup.internal.datastore.search.criteria;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class LastBaseCleanupEvaluatorTestSupport
{
  @Mock
  protected Repository repository;

  @BeforeEach
  void setup() {
    setupMocks();
  }

  protected abstract void setupMocks();

  protected Asset createMockAssetWithBlob(final OffsetDateTime blobCreatedTime) {
    Asset asset = mock(Asset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobCreated()).thenReturn(blobCreatedTime);
    return asset;
  }

  protected Asset createMockAssetWithLastDownloaded(final OffsetDateTime lastDownloadedTime) {
    Asset asset = mock(Asset.class);
    when(asset.lastDownloaded()).thenReturn(Optional.of(lastDownloadedTime));
    return asset;
  }

  protected Component createMockComponent() {
    return mock(Component.class);
  }
}
