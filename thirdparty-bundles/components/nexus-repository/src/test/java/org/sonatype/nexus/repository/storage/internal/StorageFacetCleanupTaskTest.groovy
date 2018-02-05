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
package org.sonatype.nexus.repository.storage.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.storage.StorageFacetManager

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class StorageFacetCleanupTaskTest
    extends TestSupport
{
  @Mock
  StorageFacetManager storageFacetManager

  private StorageFacetCleanupTask underTest

  @Before
  public void setUp() {
    underTest = new StorageFacetCleanupTask(storageFacetManager)
  }

  @Test
  void 'task attempts deletions until no more entries were successfully deleted'() {
    when(storageFacetManager.performDeletions()).thenReturn(2L, 1L, 0L)
    underTest.execute()
    verify(storageFacetManager, times(3)).performDeletions()
  }
}
