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

import java.util.ArrayList;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenComponentMaintenanceFacetTest
    extends TestSupport
{

  @Mock
  private EventManager eventManager;

  @Mock
  Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private EntityId entityId;

  @Mock
  private Bucket bucket;

  @Mock
  private Asset asset;

  @Mock
  private Component component;

  private MavenComponentMaintenanceFacet underTest;

  @Before
  public void setup() throws Exception {

    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(EventManager.class).toInstance(eventManager);
      }
    }).getInstance(MavenComponentMaintenanceFacet.class);

    underTest.attach(repository);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    when(storageTx.findAsset(entityId)).thenReturn(asset);
    when(storageTx.findAsset(entityId, bucket)).thenReturn(asset);
    when(storageTx.findBucket(any())).thenReturn(bucket);
    when(storageTx.findComponent(any())).thenReturn(component);
    when(storageTx.findComponentInBucket(any(), eq(bucket))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(new ArrayList<>());

    UnitOfWork.begin(() -> storageTx);
  }

  @Test
  public void deleteComponentIfLastAssetDeleted() {
    when(asset.componentId()).thenReturn(entityId);

    underTest.deleteAssetTx(entityId, true);

    verify(storageTx).deleteComponent(component, true);
  }

  @Test
  public void doNotDeleteComponentWhenOtherAssetsExist() {
    when(storageTx.browseAssets(component)).thenReturn(singletonList(asset));

    underTest.deleteAssetTx(entityId, true);

    verify(storageTx, never()).deleteComponent(component, true);
  }
}
