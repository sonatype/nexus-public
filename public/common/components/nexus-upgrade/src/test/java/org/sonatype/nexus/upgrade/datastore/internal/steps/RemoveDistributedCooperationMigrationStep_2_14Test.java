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
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RemoveDistributedCooperationMigrationStep_2_14Test
{
  @Mock
  private Connection connection;

  private RemoveDistributedCooperationMigrationStep_2_14 underTest;

  @Before
  public void setup() {
    underTest =
        new RemoveDistributedCooperationMigrationStep_2_14();
  }

  @Test
  public void testNoAction() throws Exception {
    // as this upgrade step has become a noop, just validate that nothing actually happens
    underTest.migrate(connection);
    verifyNoInteractions(connection);
  }
}
