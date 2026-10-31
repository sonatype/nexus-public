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
package org.sonatype.nexus.security.session;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.nexus.NexusWebSessionManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusSessionInvalidatorImplTest
{
  private static final String PRINCIPALS_SESSION_KEY =
      "org.apache.shiro.subject.support.DefaultSubjectContext_PRINCIPALS_SESSION_KEY";

  @Mock
  private NexusWebSessionManager sessionManager;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private CachingSessionDAO cachingSessionDAO;

  @Mock
  private Cache<Serializable, Session> cache;

  private NexusSessionInvalidatorImpl underTest;

  @Before
  public void setup() {
    underTest = new NexusSessionInvalidatorImpl(sessionManager, auditRecorder);
  }

  @Test
  public void invalidate_deletesOnlyMatchingUserSessions() {
    Session aliceSession = sessionWithPrincipal("alice");
    Session bobSession = sessionWithPrincipal("bob");
    Session aliceSession2 = sessionWithPrincipal("alice");

    when(sessionManager.getSessionDAO()).thenReturn(cachingSessionDAO);
    when(cachingSessionDAO.getActiveSessionsCache()).thenReturn(cache);
    when(cache.values()).thenReturn(Arrays.asList(aliceSession, bobSession, aliceSession2));

    int count = underTest.invalidateSessionsForUser("alice", "default", "password change");

    assertThat(count, is(2));
    verify(cachingSessionDAO).delete(aliceSession);
    verify(cachingSessionDAO).delete(aliceSession2);
    verify(cachingSessionDAO, never()).delete(bobSession);
  }

  @Test
  public void invalidate_sessionDaoNotCaching_returnsZero() {
    SessionDAO nonCachingDAO = mock(SessionDAO.class);
    when(sessionManager.getSessionDAO()).thenReturn(nonCachingDAO);

    int count = underTest.invalidateSessionsForUser("alice", "default", "password change");

    assertThat(count, is(0));
    verify(nonCachingDAO, never()).delete(any(Session.class));
  }

  @Test
  public void invalidate_nullCache_returnsZero() {
    when(sessionManager.getSessionDAO()).thenReturn(cachingSessionDAO);
    when(cachingSessionDAO.getActiveSessionsCache()).thenReturn(null);

    int count = underTest.invalidateSessionsForUser("alice", "default", "password change");

    assertThat(count, is(0));
    verify(cachingSessionDAO, never()).delete(any(Session.class));
  }

  @Test
  public void invalidate_auditRecorded_whenCountPositive() {
    Session aliceSession = sessionWithPrincipal("alice");
    when(sessionManager.getSessionDAO()).thenReturn(cachingSessionDAO);
    when(cachingSessionDAO.getActiveSessionsCache()).thenReturn(cache);
    when(cache.values()).thenReturn(Collections.singletonList(aliceSession));
    when(auditRecorder.isEnabled()).thenReturn(true);

    underTest.invalidateSessionsForUser("alice", "default", "password change");

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());
    AuditData data = captor.getValue();
    assertThat(data.getDomain(), is("security.session"));
    assertThat(data.getType(), is("user-session-invalidation"));
    assertThat(data.getAttributes().get("username"), is("alice"));
    assertThat(data.getAttributes().get("sessionType"), is("session"));
    assertThat(data.getAttributes().get("sessionCount"), is("1"));
    assertThat(data.getAttributes().get("reason"), is("password change"));
  }

  @Test
  public void invalidate_auditNotRecorded_whenNoSessionsDeleted() {
    Session bobSession = sessionWithPrincipal("bob");
    when(sessionManager.getSessionDAO()).thenReturn(cachingSessionDAO);
    when(cachingSessionDAO.getActiveSessionsCache()).thenReturn(cache);
    when(cache.values()).thenReturn(Collections.singletonList(bobSession));
    when(auditRecorder.isEnabled()).thenReturn(true);

    underTest.invalidateSessionsForUser("alice", "default", "password change");

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  private static Session sessionWithPrincipal(final String username) {
    Session session = mock(Session.class);
    PrincipalCollection principals = new SimplePrincipalCollection(username, "anyRealm");
    when(session.getAttribute(PRINCIPALS_SESSION_KEY)).thenReturn(principals);
    return session;
  }
}
