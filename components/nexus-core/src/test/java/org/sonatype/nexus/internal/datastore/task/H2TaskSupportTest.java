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
package org.sonatype.nexus.internal.datastore.task;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.CancelableHelper;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class H2TaskSupportTest
    extends TestSupport
{
  @Test
  public void testExportDatabase_HappyPath() throws Exception {
    try (ResultSet resultSet = mock(ResultSet.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class)) {
      when(connection.prepareStatement(any())).thenReturn(statement);
      when(statement.getResultSet()).thenReturn(resultSet);

      H2TaskSupport taskSupport = spy(new H2TaskSupport());
      doReturn(-1L).when(taskSupport).processResults(any(), any(), any());

      long result = taskSupport.exportDatabase(connection, "", null);

      assertEquals("Incorrect result returned", -1L, result);
      verify(taskSupport, times(1)).processResults(resultSet, "", null);
    }
  }

  @Test
  public void testProcessResults_HappyPath() throws Exception {
    // Mock the FileWriter construction to avoid unplanned I/O.
    try (ResultSet resultSet = mock(ResultSet.class);
        MockedConstruction<FileWriter> mockFileWriter = mockConstruction(FileWriter.class)) {
      H2TaskSupport taskSupport = spy(new H2TaskSupport());
      doReturn(-2L).when(taskSupport).writeLines(eq(resultSet), isNull(), anyInt(), anyInt(), any());

      long result = taskSupport.processResults(resultSet, "validLocation", null);

      assertEquals("Incorrect result returned", -1L, result);
      verify(taskSupport, times(1)).writeLines(eq(resultSet), isNull(), anyInt(), anyInt(), any());
    }
  }

  @Test
  public void testWriteLines_HappyPath() throws Exception {
    Consumer<String> testProgress = spy(TestProgress.class);
    try (ResultSet resultSet = mock(ResultSet.class);
        BufferedWriter buffer = mock(BufferedWriter.class)) {

      // Just process one line
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString(1)).thenReturn("");

      H2TaskSupport taskSupport = spy(new H2TaskSupport());

      long result = taskSupport.writeLines(resultSet, testProgress, 1, 1, buffer);

      assertEquals("Incorrect result returned", 1L, result);
      verify(buffer, times(2)).write(anyString());
      verify(buffer, times(2)).newLine();
      verify(buffer, times(1)).flush();
      verify(taskSupport, times(1)).updateProgress(testProgress, 1L);
    }
  }

  @Test
  public void testUpdateProgress_HappyPath() throws Exception {
    Consumer<String> testProgress = spy(TestProgress.class);
    try (MockedStatic<CancelableHelper> cancelHelper = mockStatic(CancelableHelper.class)) {

      H2TaskSupport taskSupport = spy(new H2TaskSupport());
      when(taskSupport.processResults(any(), any(), any())).thenReturn(-1L);

      taskSupport.updateProgress(testProgress, 0);

      verify(testProgress, times(1)).accept(any());
      cancelHelper.verify(CancelableHelper::checkCancellation, times(1));
    }
  }

  @Test
  public void testRollback_HappyPath() throws Exception {
    try (Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class)) {
      when(connection.prepareStatement(any())).thenReturn(statement);

      H2TaskSupport taskSupport = spy(new H2TaskSupport());

      taskSupport.rollback(connection);

      verify(connection, times(1)).rollback();
    }
  }

  @Test
  public void testGetAndSetAutoCommit_HappyPath() throws Exception {
    try (Connection connection = mock(Connection.class)) {
      when(connection.getAutoCommit()).thenReturn(true);

      H2TaskSupport taskSupport = spy(new H2TaskSupport());

      boolean result = taskSupport.getAndSetAutoCommit(connection);

      assertTrue("Incorrect result returned", result);
      verify(connection, times(1)).getAutoCommit();
      verify(connection, times(1)).setAutoCommit(false);
    }

  }

  @Test
  public void testResetAutoCommit_HappyPath() throws Exception {
    try (Connection connection = mock(Connection.class)) {

      H2TaskSupport taskSupport = spy(new H2TaskSupport());

      taskSupport.resetAutoCommit(connection, true);
      verify(connection, times(1)).setAutoCommit(true);

      taskSupport.resetAutoCommit(connection, false);
      verify(connection, times(1)).setAutoCommit(false);
    }
  }

  private static class TestProgress
      implements Consumer<String>
  {
    @Override
    public void accept(final String s) {
      // Ignore
    }
  }
}
