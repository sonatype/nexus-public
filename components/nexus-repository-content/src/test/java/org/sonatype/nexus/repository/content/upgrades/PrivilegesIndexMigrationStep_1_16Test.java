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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class PrivilegesIndexMigrationStep_1_16Test
    extends TestSupport
{
  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  private PrivilegesIndexMigrationStep_1_16 migration;

  @Before
  public void setUp() throws SQLException {
    migration = new PrivilegesIndexMigrationStep_1_16();
    Mockito.when(connection.createStatement()).thenReturn(statement);
  }

  @Test
  public void testCreateIndexSuccessfully() throws Exception {
    migration.migrate(connection);

    Mockito.verify(connection, Mockito.times(1)).createStatement();
    Mockito.verify(statement, Mockito.times(1)).execute(Mockito.anyString());
  }

  @Test
  public void testCreateIndexFails() throws Exception {
    SQLException expected = null;
    try {
      Mockito.when(statement.execute(Mockito.anyString())).thenThrow(new SQLException("test exception"));

      migration.migrate(connection);
    }
    catch (SQLException sqlException) {
      expected = sqlException;
    }
    Assert.assertNotNull(expected);
    Assert.assertEquals("test exception" , expected.getMessage());
    Mockito.verify(connection, Mockito.times(1)).createStatement();
    Mockito.verify(statement, Mockito.times(1)).execute(Mockito.anyString());
  }
}
