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
package org.sonatype.nexus.internal.jwt.datastore;

import java.time.OffsetDateTime;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@ExtendWith(MockitoExtension.class)
class JwtSessionStoreTest
{
  @Mock
  private DataSessionSupplier sessionSupplier;

  @SuppressWarnings("rawtypes")
  @Mock
  private DataSession dataSession;

  @Mock
  private Transaction transaction;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private JwtSessionDAO dao;

  private JwtSessionStore underTest;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    lenient().when(sessionSupplier.openSession(DEFAULT_DATASTORE_NAME)).thenReturn(dataSession);
    lenient().when(dataSession.access(JwtSessionDAO.class)).thenReturn(dao);
    lenient().when(dataSession.getTransaction()).thenReturn(transaction);
    UnitOfWork.beginBatch(dataSession);
    underTest = new JwtSessionStore(sessionSupplier, databaseCheck);
  }

  @AfterEach
  void teardown() {
    UnitOfWork.end();
  }

  @Test
  void isRevoked_usesTypedQuery_whenSchemaAtLeast2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(true);
    when(dao.isRevoked("session-1")).thenReturn(true);

    boolean result = underTest.isRevoked("session-1");

    assertThat(result, is(true));
    verify(dao).isRevoked("session-1");
    verify(dao, never()).isRevokedLegacy(any());
  }

  @Test
  void isRevoked_usesLegacyQuery_whenSchemaBelow2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(false);
    when(dao.isRevokedLegacy("session-1")).thenReturn(true);

    boolean result = underTest.isRevoked("session-1");

    assertThat(result, is(true));
    verify(dao).isRevokedLegacy("session-1");
    verify(dao, never()).isRevoked(any());
  }

  @Test
  void revokeSession_usesTypedInsert_whenSchemaAtLeast2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(true);

    underTest.revokeSession("session-1", "admin", "default", OffsetDateTime.now().plusHours(1));

    verify(dao).revokeSession(any(JwtSessionData.class));
    verify(dao, never()).revokeSessionLegacy(any());
  }

  @Test
  void revokeSession_usesLegacyInsert_whenSchemaBelow2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(false);

    underTest.revokeSession("session-1", "admin", "default", OffsetDateTime.now().plusHours(1));

    verify(dao).revokeSessionLegacy(any(JwtSessionData.class));
    verify(dao, never()).revokeSession(any());
  }

  @Test
  void invalidateUser_skips_whenSchemaBelow2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(false);

    underTest.invalidateUser("admin", "default", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

    verify(dao, never()).invalidateUser(any());
  }

  @Test
  void invalidateUser_proceeds_whenSchemaAtLeast2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(true);

    underTest.invalidateUser("admin", "default", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

    verify(dao).invalidateUser(any(JwtSessionData.class));
  }

  @Test
  void isUserInvalidatedAfter_returnsFalse_whenSchemaBelow2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(false);

    boolean result = underTest.isUserInvalidatedAfter("admin", OffsetDateTime.now());

    assertThat(result, is(false));
    verify(dao, never()).isUserInvalidatedAfter(any(), any());
  }

  @Test
  void isUserInvalidatedAfter_queries_whenSchemaAtLeast2_127() {
    when(databaseCheck.isAtLeast("2.127")).thenReturn(true);
    when(dao.isUserInvalidatedAfter(any(), any())).thenReturn(true);

    boolean result = underTest.isUserInvalidatedAfter("admin", OffsetDateTime.now());

    assertThat(result, is(true));
    verify(dao).isUserInvalidatedAfter(any(), any());
  }
}
