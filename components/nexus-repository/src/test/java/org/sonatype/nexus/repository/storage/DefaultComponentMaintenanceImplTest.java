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
package org.sonatype.nexus.repository.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultComponentMaintenanceImplTest
    extends TestSupport
{
  @Mock
  private DeletionProgress deleteProgress;

  @Mock
  private EntityId entityId1, entityId2;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx tx;

  private List<EntityId> entityIds;

  private DefaultComponentMaintenanceImpl underTest;

  @Before
  public void setUp() throws Exception {
    entityIds = Arrays.asList(entityId1, entityId2);

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);

    underTest = new DefaultComponentMaintenanceImpl()
    {
      @Override
      protected DeletionProgress doBatchDelete(final List<EntityId> entityIds, final BooleanSupplier cancelledCheck) {
        return deleteProgress;
      }
    };
    underTest.attach(repository);
  }

  @Test
  public void catchAfterExceptionDuringDeleteComponents() throws Exception {
    DefaultComponentMaintenanceImpl defaultComponentMaintenance = new DefaultComponentMaintenanceImpl()
    {
      @Override
      public void after() {
        throw new RuntimeException();
      }
    };

    defaultComponentMaintenance.attach(repository);

    defaultComponentMaintenance.deleteComponents(new ArrayList<>(), () -> false, 1);
  }

  @Test
  public void batchSuccessCompletesDeletion() throws Exception {
    when(deleteProgress.isFailed()).thenReturn(false);

    DeletionProgress deleteComponentsProgress = underTest.deleteComponents(entityIds, () -> false, 1);

    assertThat(deleteComponentsProgress.isFailed(), is(false));

    verify(deleteProgress, times(2)).getCount();
  }

  @Test
  public void batchFailureFailsDeletion() throws Exception {
    when(deleteProgress.isFailed()).thenReturn(true);
    DeletionProgress deleteComponentsProgress = underTest.deleteComponents(entityIds, () -> false, 1);

    assertThat(deleteComponentsProgress.isFailed(), is(true));

    verify(deleteProgress).getCount();
  }
}

