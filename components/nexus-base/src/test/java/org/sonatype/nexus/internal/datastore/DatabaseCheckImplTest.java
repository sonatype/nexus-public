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
package org.sonatype.nexus.internal.datastore;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DatabaseCheckImplTest
    extends TestSupport
{
  private DatabaseCheckImpl underTest;

  @Mock
  private DataStoreManager dataStoreManager;

  private WithAnnotation withAnnotation;

  private WithoutAnnotation withoutAnnotation;

  private WithFromAnnotation withFromAnnotation;

  private WithLatestFromAnnotation withLatestFromAnnotation;

  @Mock
  private Optional<DataStore<?>> optionalDataStore;

  @Mock
  private DataStore dataStore;

  @Before
  public void setup() {
    when(dataStoreManager.get(any())).thenReturn(optionalDataStore);
    when(optionalDataStore.orElseThrow(any())).thenReturn(dataStore);

    withAnnotation = new WithAnnotation();
    withoutAnnotation = new WithoutAnnotation();
    withFromAnnotation = new WithFromAnnotation();
    withLatestFromAnnotation = new WithLatestFromAnnotation();
  }

  @Test
  public void testIsAllowedByVersion() {
    underTest = new DatabaseCheckImplForTest(dataStoreManager);

    assertTrue(underTest.isAllowedByVersion(withAnnotation.getClass()));
    assertFalse(underTest.isAllowedByVersion(withoutAnnotation.getClass()));
    assertTrue(underTest.isAllowedByVersion(withFromAnnotation.getClass()));
    assertFalse(underTest.isAllowedByVersion(withLatestFromAnnotation.getClass()));
  }

  private static class DatabaseCheckImplForTest
      extends DatabaseCheckImpl
  {
    public DatabaseCheckImplForTest(
        final DataStoreManager dataStoreManager)
    {
      super(dataStoreManager);
    }

    @Override
    MigrationVersion getMigrationVersion() {
      return MigrationVersion.fromVersion("1.4");
    }
  }

  private static class WithoutAnnotation
  {
  }

  @AvailabilityVersion(from = "1.0")
  private static class WithAnnotation
  {
  }

  @AvailabilityVersion(from = "1.3")
  private static class WithFromAnnotation
  {
  }

  @AvailabilityVersion(from = "1.6")
  private static class WithLatestFromAnnotation
  {
  }

}
