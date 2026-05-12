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
