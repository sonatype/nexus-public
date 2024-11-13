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
package org.sonatype.nexus.repository.content.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseIntegrityCheckServiceTest
    extends TestSupport
{
  @Mock
  private DataSessionSupplier dataSessionSupplier;

  @Mock
  private List<DatabaseIntegrityChecker> databaseIntegrityCheckers;

  @Mock
  private Connection connection;

  @InjectMocks
  private DatabaseIntegrityCheckService databaseIntegrityCheckService;

  @Before
  public void mockDatabaseConnection() throws Exception {
    when(dataSessionSupplier.openConnection(anyString())).thenReturn(connection);
  }

  @Test
  public void testDoStart() throws Exception {
    DatabaseIntegrityChecker checker1 = mock(DatabaseIntegrityChecker.class);
    DatabaseIntegrityChecker checker2 = mock(DatabaseIntegrityChecker.class);
    databaseIntegrityCheckers = asList(checker1, checker2);

    databaseIntegrityCheckService = new DatabaseIntegrityCheckService(dataSessionSupplier, databaseIntegrityCheckers);
    databaseIntegrityCheckService.doStart();

    verify(checker1).checkAndRepair(connection);
    verify(checker2).checkAndRepair(connection);
  }

  @Test
  public void doStartThrowsExceptions() throws Exception {
    DatabaseIntegrityChecker checker = mock(DatabaseIntegrityChecker.class);
    doThrow(SQLException.class).when(checker).checkAndRepair(connection);
    databaseIntegrityCheckers = asList(checker);

    databaseIntegrityCheckService = new DatabaseIntegrityCheckService(dataSessionSupplier, databaseIntegrityCheckers);

    assertThrows(SQLException.class, () -> databaseIntegrityCheckService.doStart());

    verify(checker).checkAndRepair(connection);
  }
}