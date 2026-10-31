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
package org.sonatype.nexus.cleanup.internal.storage.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CleanupConfigUpgrade_1_9Test
{
  /*
   * Pinned to the EXACT string produced by the production concatenation
   * ("UPDATE CLEANUP_POLICY " + " SET FORMAT='ALL_FORMATS' WHERE FORMAT='*'"),
   * which yields a (harmless) double space between CLEANUP_POLICY and SET.
   */
  private static final String EXPECTED_SQL = "UPDATE CLEANUP_POLICY  SET FORMAT='ALL_FORMATS' WHERE FORMAT='*'";

  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  private final CleanupConfigUpgrade_1_9 underTest = new CleanupConfigUpgrade_1_9();

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();

    assertThat(version.isPresent(), is(true));
    assertThat(version.get(), is("1.9"));
    assertThat(version, is(Optional.of("1.9")));
  }

  @Test
  public void testMigrateUpdatesMisconfiguredCleanupPolicies() throws Exception {
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeUpdate(anyString())).thenReturn(2);

    underTest.migrate(connection);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(statement).executeUpdate(sqlCaptor.capture());

    String sql = sqlCaptor.getValue();
    // Token assertions kept for readability/regression of the individual clauses...
    assertThat(sql, containsString("UPDATE CLEANUP_POLICY"));
    assertThat(sql, containsString("SET FORMAT='ALL_FORMATS'"));
    assertThat(sql, containsString("WHERE FORMAT='*'"));
    // ...but the exact string (including the double space) is what actually runs.
    assertThat(sql, is(EXPECTED_SQL));

    // The statement is obtained exactly once and closed by the try-with-resources.
    verify(connection).createStatement();
    verify(statement).close();
    verifyNoMoreInteractions(statement);
  }

  @Test
  public void testMigrateIssuesSqlEvenWhenNoPoliciesAreMisconfigured() throws Exception {
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeUpdate(anyString())).thenReturn(0);

    underTest.migrate(connection);

    verify(connection).createStatement();
    verify(statement).executeUpdate(EXPECTED_SQL);
    verify(statement).close();
    verifyNoMoreInteractions(statement);
  }

  @Test
  public void testMigrateClosesStatementWhenExecuteUpdateFails() throws Exception {
    when(connection.createStatement()).thenReturn(statement);
    SQLException failure = new SQLException("boom");
    when(statement.executeUpdate(anyString())).thenThrow(failure);

    try {
      underTest.migrate(connection);
      fail("Expected SQLException to propagate");
    }
    catch (SQLException e) {
      assertThat(e, sameInstance(failure));
    }

    // try-with-resources must still close the statement on failure.
    verify(statement).close();
  }
}
