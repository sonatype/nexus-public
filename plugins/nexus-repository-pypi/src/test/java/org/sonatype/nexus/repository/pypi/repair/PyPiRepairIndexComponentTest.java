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
package org.sonatype.nexus.repository.pypi.repair;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class PyPiRepairIndexComponentTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  @Mock
  private Type type;

  @Mock
  private Format format;

  @Mock
  private StorageTx tx;

  @Mock
  private Repository repository;

  @Mock
  private Asset asset;

  @Mock
  private NestedAttributesMap attributes;

  PyPiRepairIndexComponent underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new PyPiRepairIndexComponent(repositoryManager, assetEntityAdapter, type, format);

    when(asset.formatAttributes()).thenReturn(attributes);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void tearDown() throws Exception {
    UnitOfWork.end();
  }

  @Test
  public void rootIndexIsDeleted() {
    when(attributes.get(P_ASSET_KIND, String.class)).thenReturn(AssetKind.ROOT_INDEX.name());

    underTest.updateAsset(repository, tx, asset);

    verify(tx).deleteAsset(asset);
    verifyNoMoreInteractions(tx);
  }

  @Test
  public void packageIndexIsDeleted() {
    when(attributes.get(P_ASSET_KIND, String.class)).thenReturn(AssetKind.INDEX.name());

    underTest.updateAsset(repository, tx, asset);

    verify(tx).deleteAsset(asset);
    verifyNoMoreInteractions(tx);
  }

  @Test
  public void packagesAreNotDeleted() {
    when(attributes.get(P_ASSET_KIND, String.class)).thenReturn(AssetKind.PACKAGE.name());

    underTest.updateAsset(repository, tx, asset);

    verifyZeroInteractions(tx);
  }
}
