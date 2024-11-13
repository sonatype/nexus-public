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
package org.sonatype.nexus.datastore.mybatis;

import java.sql.SQLException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.datastore.api.SerializedAccessException;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntityExecutorTest
    extends TestSupport
{
  @Mock
  private Executor delegate;

  @Mock
  private FrozenChecker frozenChecker;

  private EntityExecutor underTest;

  @Before
  public void setup() {
    underTest = new EntityExecutor(delegate, frozenChecker);
  }

  @Test
  public void testCommit() throws SQLException {
    underTest.commit(true);
    verify(delegate).commit(true);

    doThrow(duplicateKeyException(), serializedAccessException(), missingStateException()).when(delegate).commit(true);
    assertThrows(DuplicateKeyException.class, () -> underTest.commit(true));
    assertThrows(SerializedAccessException.class, () -> underTest.commit(true));
    assertThrows(SQLException.class, () -> underTest.commit(true));
  }

  @Test
  public void testFlushStatements() throws SQLException {
    underTest.flushStatements();
    verify(delegate).flushStatements();

    when(delegate.flushStatements()).thenThrow(duplicateKeyException(), serializedAccessException(), missingStateException());
    assertThrows(DuplicateKeyException.class, () -> underTest.flushStatements());
    assertThrows(SerializedAccessException.class, () -> underTest.flushStatements());
    assertThrows(SQLException.class, () -> underTest.flushStatements());
  }

  @Test
  public void testQuery_4arg() throws SQLException {
    underTest.query(null, null, null, null);
    verify(delegate).query(null, null, null, null);

    when(delegate.query(null, null, null, null)).thenThrow(duplicateKeyException(), serializedAccessException(), missingStateException());
    assertThrows(DuplicateKeyException.class, () -> underTest.query(null,  null, null, null));
    assertThrows(SerializedAccessException.class, () -> underTest.query(null,  null, null, null));
    assertThrows(SQLException.class, () -> underTest.query(null,  null, null, null));
  }

  @Test
  public void testQuery_6arg() throws SQLException {
    underTest.query(null, null, null, null, null, null);
    verify(delegate).query(null, null, null, null, null, null);

    when(delegate.query(null, null, null, null, null, null)).thenThrow(duplicateKeyException(), serializedAccessException(), missingStateException());
    assertThrows(DuplicateKeyException.class, () -> underTest.query(null, null, null, null, null, null));
    assertThrows(SerializedAccessException.class, () -> underTest.query(null, null, null, null, null, null));
    assertThrows(SQLException.class, () -> underTest.query(null, null, null, null, null, null));
  }

  @Test
  public void testQueryCursor() throws SQLException {
    underTest.queryCursor(null, null, null);
    verify(delegate).queryCursor(null, null, null);

    when(delegate.queryCursor(null, null, null)).thenThrow(duplicateKeyException(), serializedAccessException(), missingStateException());
    assertThrows(DuplicateKeyException.class, () -> underTest.queryCursor(null, null, null));
    assertThrows(SerializedAccessException.class, () -> underTest.queryCursor(null, null, null));
    assertThrows(SQLException.class, () -> underTest.queryCursor(null, null, null));
  }

  @Test
  public void testRollback() throws SQLException {
    underTest.rollback(true);
    verify(delegate).rollback(true);

    doThrow(duplicateKeyException(), serializedAccessException(), missingStateException()).when(delegate).rollback(true);
    assertThrows(DuplicateKeyException.class, () -> underTest.rollback(true));
    assertThrows(SerializedAccessException.class, () -> underTest.rollback(true));
    assertThrows(SQLException.class, () -> underTest.rollback(true));
  }

  @Test
  public void testUpdate() throws SQLException {
    MappedStatement ms = mock(MappedStatement.class);
    underTest.update(ms, null);
    verify(delegate).update(ms, null);

    when(delegate.update(ms, null)).thenThrow(duplicateKeyException(), serializedAccessException(), missingStateException());
    assertThrows(DuplicateKeyException.class, () -> underTest.update(ms, null));
    assertThrows(SerializedAccessException.class, () -> underTest.update(ms, null));
    assertThrows(SQLException.class, () -> underTest.update(ms, null));
  }

  @Test
  public void testUpdate_frozen() throws SQLException {
    MappedStatement ms = mock(MappedStatement.class);
    doThrow(new FrozenException("Frozen")).when(frozenChecker).checkFrozen(ms);

    assertThrows(FrozenException.class, () -> underTest.update(ms, null));
    verify(delegate, never()).update(ms, null);
  }

  private static SQLException duplicateKeyException() {
    return new SQLException("Duplicate Key", DuplicateKeyException.SQL_STATE);
  }

  private static SQLException serializedAccessException() {
    return new SQLException("Isolation", SerializedAccessException.SQL_STATE);
  }

  private static SQLException missingStateException() {
    return new SQLException("Some hikari error");
  }
}
