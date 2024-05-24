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
package org.sonatype.nexus.repository.content.browse.store;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.internal.AssetAuditor;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetAuditorTest extends TestSupport {
  AssetAuditor assetAuditor;

  @Test
  public void testAuditChangesDetailEnabled() {
    assetAuditor = Mockito.spy(new AssetAuditor(true));

    AssetData assetData = new AssetData();
    assetData.setRepositoryId(1);

    Repository repository = mock(Repository.class);

    AssetAttributesEvent assetAttributesEvent =  mock(AssetAttributesEvent.class);
    when(assetAttributesEvent.getAsset()).thenReturn(assetData);
    when(assetAttributesEvent.getRepository()).thenReturn(Optional.of(repository));

    AuditRecorder auditRecorder = mock(AuditRecorder.class);
    when(auditRecorder.isEnabled()).thenReturn(true);
    assetAuditor.setAuditRecorder(() -> auditRecorder);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      assetAuditor.on(assetAttributesEvent);
      verify(assetAttributesEvent, times(1)).getChanges();
    }

    verify(auditRecorder, times(1)).record(any());
  }

  @Test
  public void testAuditChangesDetailDisabled() {
    assetAuditor = Mockito.spy(new AssetAuditor(false));

    AssetData assetData = new AssetData();
    assetData.setRepositoryId(1);

    Repository repository = mock(Repository.class);

    AssetAttributesEvent assetAttributesEvent =  mock(AssetAttributesEvent.class);
    when(assetAttributesEvent.getAsset()).thenReturn(assetData);
    when(assetAttributesEvent.getRepository()).thenReturn(Optional.of(repository));

    AuditRecorder auditRecorder = mock(AuditRecorder.class);
    when(auditRecorder.isEnabled()).thenReturn(true);

    assetAuditor.setAuditRecorder(() -> auditRecorder);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      assetAuditor.on(assetAttributesEvent);
      verify(assetAttributesEvent, times(0)).getChanges();
    }

    verify(auditRecorder, times(1)).record(any());

  }
}
