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
package org.sonatype.nexus.cleanup.internal.orient.method;

import java.util.function.BooleanSupplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrientDeleteCleanupMethodTest
    extends TestSupport
{
  private static final int BATCH_SIZE = 500;

  @Mock
  private Repository repository;

  @Mock
  private EntityId component1, component2;

  @Mock
  private BooleanSupplier cancelledCheck;

  @Mock
  private ComponentMaintenance componentMaintenance;

  private OrientDeleteCleanupMethod underTest;

  @Before
  public void setup() throws Exception {
    underTest = new OrientDeleteCleanupMethod(500);

    when(cancelledCheck.getAsBoolean()).thenReturn(false);
    when(repository.facet(ComponentMaintenance.class)).thenReturn(componentMaintenance);
  }

  @Test
  public void deleteComponent() throws Exception {
    DeletionProgress deletionProgress = new DeletionProgress();
    deletionProgress.addComponentCount(2L);

    when(componentMaintenance.deleteComponents(any(), any(), anyInt())).thenReturn(deletionProgress);

    DeletionProgress response =
        underTest.run(repository, ImmutableList.of(component1, component2).stream(), cancelledCheck);

    assertThat(response.getComponentCount(), equalTo(2L));

    verify(componentMaintenance).deleteComponents(eq(ImmutableList.of(component1, component2)), eq(cancelledCheck),
        eq(BATCH_SIZE));
  }
}
