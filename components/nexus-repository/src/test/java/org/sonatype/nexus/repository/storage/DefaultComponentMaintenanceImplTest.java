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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;

import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class DefaultComponentMaintenanceImplTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;
  
  @Mock
  private StorageTx tx;

  @Test
  public void catchAfterExceptionDuringDeleteComponents() throws Exception {
    DefaultComponentMaintenanceImpl defaultComponentMaintenance = new DefaultComponentMaintenanceImpl()
    {
      @Override
      public void after() {
        throw new RuntimeException();
      }
    };

    setup(defaultComponentMaintenance);

    defaultComponentMaintenance.deleteComponents(new ArrayList<>(), () -> false, 1);
  }

  private void setup(final DefaultComponentMaintenanceImpl defaultComponentMaintenance) throws Exception {
    defaultComponentMaintenance.attach(repository);

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    
    when(storageFacet.txSupplier()).thenReturn(() -> tx);
  }
}
