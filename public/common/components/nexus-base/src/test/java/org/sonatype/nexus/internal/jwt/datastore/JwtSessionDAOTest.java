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
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class JwtSessionDAOTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(JwtSessionDAO.class);

  private DataSession<?> session;

  private JwtSessionDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(JwtSessionDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testRevokeAndCheckSession() {
    // Initially not revoked
    boolean initiallyRevoked = withDao(d -> d.isRevoked("session-123"));
    assertThat(initiallyRevoked, is(false));

    // Revoke the session
    JwtSessionData sessionData = new JwtSessionData();
    sessionData.setUserSessionId("session-123");
    sessionData.setUsername("testuser");
    sessionData.setUserSource("default");
    sessionData.setRevokedAt(OffsetDateTime.now());
    sessionData.setExpiresAt(OffsetDateTime.now().plusHours(1));

    callDao(d -> d.revokeSession(sessionData));

    // Now it should be revoked
    boolean afterRevocation = withDao(d -> d.isRevoked("session-123"));
    assertThat(afterRevocation, is(true));
  }

  @Test
  public void testRevokeSessionIdempotency() {
    // Create session data
    JwtSessionData sessionData = new JwtSessionData();
    sessionData.setUserSessionId("session-456");
    sessionData.setUsername("testuser");
    sessionData.setUserSource("default");
    sessionData.setRevokedAt(OffsetDateTime.now());
    sessionData.setExpiresAt(OffsetDateTime.now().plusHours(1));

    // Revoke twice - should not error due to ON CONFLICT DO NOTHING
    callDao(d -> d.revokeSession(sessionData));
    callDao(d -> d.revokeSession(sessionData));

    // Should still be revoked
    boolean revoked = withDao(d -> d.isRevoked("session-456"));
    assertThat(revoked, is(true));
  }

  @Test
  public void testDeleteExpiredSessions() {
    // Add expired session
    JwtSessionData expiredSession = new JwtSessionData();
    expiredSession.setUserSessionId("expired-session");
    expiredSession.setUsername("testuser");
    expiredSession.setUserSource("default");
    expiredSession.setRevokedAt(OffsetDateTime.now().minusHours(2));
    expiredSession.setExpiresAt(OffsetDateTime.now().minusHours(1)); // Expired 1 hour ago

    callDao(d -> d.revokeSession(expiredSession));

    // Add non-expired session
    JwtSessionData activeSession = new JwtSessionData();
    activeSession.setUserSessionId("active-session");
    activeSession.setUsername("testuser");
    activeSession.setUserSource("default");
    activeSession.setRevokedAt(OffsetDateTime.now());
    activeSession.setExpiresAt(OffsetDateTime.now().plusHours(1)); // Expires in 1 hour

    callDao(d -> d.revokeSession(activeSession));

    // Both should be revoked initially
    assertThat(withDao(d -> d.isRevoked("expired-session")), is(true));
    assertThat(withDao(d -> d.isRevoked("active-session")), is(true));

    // Delete expired sessions
    int deleted = withDao(JwtSessionDAO::deleteExpiredSessions);
    assertThat(deleted, is(1));

    // Expired session should be gone, active should remain
    assertThat(withDao(d -> d.isRevoked("expired-session")), is(false));
    assertThat(withDao(d -> d.isRevoked("active-session")), is(true));
  }

  @Test
  public void testNonExistentSession() {
    boolean revoked = withDao(d -> d.isRevoked("non-existent-session"));
    assertThat(revoked, is(false));
  }

  @Test
  public void testInvalidateUserAndIsUserInvalidatedAfter() {
    OffsetDateTime cutoff = OffsetDateTime.now();

    // No invalidation yet
    assertThat(withDao(d -> d.isUserInvalidatedAfter("alice", cutoff.minusMinutes(5))), is(false));

    // Record user-wide invalidation (user_source stored but not used in match)
    JwtSessionData invalidation = new JwtSessionData();
    invalidation.setUserSessionId(UUID.randomUUID().toString());
    invalidation.setUsername("alice");
    invalidation.setUserSource("default");
    invalidation.setRevokedAt(cutoff);
    invalidation.setExpiresAt(cutoff.plusHours(1));
    invalidation.setType(JwtSessionData.TYPE_USER_INVALIDATION);
    callDao(d -> d.invalidateUser(invalidation));

    // JWT issued BEFORE the cutoff → invalidated
    assertThat(withDao(d -> d.isUserInvalidatedAfter("alice", cutoff.minusMinutes(5))), is(true));

    // JWT issued AFTER the cutoff → not invalidated
    assertThat(withDao(d -> d.isUserInvalidatedAfter("alice", cutoff.plusMinutes(5))), is(false));

    // Different username → not invalidated
    assertThat(withDao(d -> d.isUserInvalidatedAfter("bob", cutoff.minusMinutes(5))), is(false));
  }

  @Test
  public void testIsRevokedDoesNotMatchUserInvalidationRows() {
    // Insert only a USER_INVALIDATION row
    String syntheticId = UUID.randomUUID().toString();
    JwtSessionData invalidation = new JwtSessionData();
    invalidation.setUserSessionId(syntheticId);
    invalidation.setUsername("alice");
    invalidation.setUserSource("default");
    invalidation.setRevokedAt(OffsetDateTime.now());
    invalidation.setExpiresAt(OffsetDateTime.now().plusHours(1));
    invalidation.setType(JwtSessionData.TYPE_USER_INVALIDATION);
    callDao(d -> d.invalidateUser(invalidation));

    // isRevoked is scoped to type='SESSION' and must not match this row
    assertThat(withDao(d -> d.isRevoked(syntheticId)), is(false));
  }

  @Test
  public void testRevokeSessionLegacyAndIsRevokedLegacy() {
    // Initially not revoked
    assertThat(withDao(d -> d.isRevokedLegacy("legacy-session-1")), is(false));

    // Revoke using the legacy path (no type column)
    JwtSessionData sessionData = new JwtSessionData();
    sessionData.setUserSessionId("legacy-session-1");
    sessionData.setUsername("testuser");
    sessionData.setUserSource("default");
    sessionData.setRevokedAt(OffsetDateTime.now());
    sessionData.setExpiresAt(OffsetDateTime.now().plusHours(1));
    callDao(d -> d.revokeSessionLegacy(sessionData));

    // Now it should be revoked via legacy check
    assertThat(withDao(d -> d.isRevokedLegacy("legacy-session-1")), is(true));
  }

  @Test
  public void testDeleteExpiredRemovesBothRowKinds() {
    // Expired SESSION row
    JwtSessionData expiredSession = new JwtSessionData();
    expiredSession.setUserSessionId("expired-session-row");
    expiredSession.setUsername("alice");
    expiredSession.setUserSource("default");
    expiredSession.setRevokedAt(OffsetDateTime.now().minusHours(2));
    expiredSession.setExpiresAt(OffsetDateTime.now().minusHours(1));
    callDao(d -> d.revokeSession(expiredSession));

    // Expired USER_INVALIDATION row
    String expiredInvId = UUID.randomUUID().toString();
    JwtSessionData expiredInvalidation = new JwtSessionData();
    expiredInvalidation.setUserSessionId(expiredInvId);
    expiredInvalidation.setUsername("alice");
    expiredInvalidation.setUserSource("default");
    expiredInvalidation.setRevokedAt(OffsetDateTime.now().minusHours(2));
    expiredInvalidation.setExpiresAt(OffsetDateTime.now().minusHours(1));
    expiredInvalidation.setType(JwtSessionData.TYPE_USER_INVALIDATION);
    callDao(d -> d.invalidateUser(expiredInvalidation));

    // Active USER_INVALIDATION row
    String activeInvId = UUID.randomUUID().toString();
    JwtSessionData activeInvalidation = new JwtSessionData();
    activeInvalidation.setUserSessionId(activeInvId);
    activeInvalidation.setUsername("bob");
    activeInvalidation.setUserSource("default");
    activeInvalidation.setRevokedAt(OffsetDateTime.now());
    activeInvalidation.setExpiresAt(OffsetDateTime.now().plusHours(1));
    activeInvalidation.setType(JwtSessionData.TYPE_USER_INVALIDATION);
    callDao(d -> d.invalidateUser(activeInvalidation));

    int deleted = withDao(JwtSessionDAO::deleteExpiredSessions);
    assertThat(deleted, is(2));

    // Active invalidation remains
    assertThat(withDao(d -> d.isUserInvalidatedAfter("bob",
        OffsetDateTime.now().minusMinutes(1))), is(true));
  }

  private void callDao(final Consumer<JwtSessionDAO> fn) {
    fn.accept(dao);
    session.getTransaction().commit();
  }

  private <T> T withDao(final Function<JwtSessionDAO, T> fn) {
    T result = fn.apply(dao);
    session.getTransaction().commit();
    return result;
  }
}
