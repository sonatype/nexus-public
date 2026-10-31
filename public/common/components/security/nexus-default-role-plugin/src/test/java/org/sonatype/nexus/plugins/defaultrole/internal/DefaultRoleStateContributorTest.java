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
package org.sonatype.nexus.plugins.defaultrole.internal;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultRoleStateContributorTest
{

  @Mock
  private DefaultRoleRealm defaultRoleRealm;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private Subject subject;

  @Mock
  private RealmManager realmManager;

  @InjectMocks
  private DefaultRoleStateContributor underTest;

  private Role defaultRole;

  private ThreadState subjectThreadState;

  @Before
  public void setupDefaultRoleRealm() {
    defaultRole = new Role();
    defaultRole.setRoleId("id");
    defaultRole.setName("name");

    when(defaultRoleRealm.getRole()).thenReturn(defaultRole.getRoleId());
  }

  @After
  public void clearSubject() {
    if (subjectThreadState != null) {
      subjectThreadState.clear();
    }
  }

  @Test
  public void unauthenticatedUserDoesNotGetDefaultRoleState() {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    Subject subject = mock(Subject.class);
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);
    setSubject(subject);

    Map<String, Object> state = underTest.getState();

    assertThat(state, is(emptyMap()));
  }

  @Test
  public void authenticatedUserGetsTheDefaultRoleState() throws NoSuchAuthorizationManagerException {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    Subject subject = mock(Subject.class);
    when(subject.isAuthenticated()).thenReturn(true);
    setSubject(subject);

    when(securitySystem.listRoles(DEFAULT_SOURCE)).thenReturn(singleton(defaultRole));

    Map<String, Object> state = underTest.getState();
    Map<String, Object> defaultRoleState = (Map<String, Object>) state.get("defaultRole");

    assertThat(defaultRoleState.get("id"), is(defaultRole.getRoleId()));
    assertThat(defaultRoleState.get("name"), is(defaultRole.getName()));
  }

  /**
   * NEXUS-53915: when the Default Role realm is enabled and configured, but the configured role has been deleted,
   * {@code securitySystem.listRoles(...)} no longer contains it so the stream lookup resolves to {@code null}.
   * Previously the code dereferenced that {@code null} ({@code matched.getRoleId()}), threw a
   * {@link NullPointerException}, and swallowed it at DEBUG - leaving no trace at default log levels. After the fix
   * it must return a clean empty state (default role simply omitted) AND emit a WARN naming the missing role so the
   * misconfiguration is visible to ERROR/WARN log filters. Because {@link DefaultRoleStateContributor#getState()} is
   * polled every few seconds by every authenticated UI session, the WARN must be logged only once for a given
   * missing role - not on every poll - to avoid flooding the log.
   */
  @Test
  public void configuredDefaultRoleMissingLogsWarningAndReturnsEmptyState() throws NoSuchAuthorizationManagerException {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    when(defaultRoleRealm.getRole()).thenReturn("deleted-default-role");
    Subject subject = mock(Subject.class);
    when(subject.isAuthenticated()).thenReturn(true);
    setSubject(subject);

    // The configured default role ("deleted-default-role") has been deleted: listRoles() only returns a
    // different role, so the stream lookup yields null.
    Role someOtherRole = new Role();
    someOtherRole.setRoleId("some-other-role");
    someOtherRole.setName("Some Other Role");
    when(securitySystem.listRoles(DEFAULT_SOURCE)).thenReturn(singleton(someOtherRole));

    // Capture real log events from the contributor's logger (logback singleton, same instance used in production).
    Logger contributorLogger = (Logger) LoggerFactory.getLogger(DefaultRoleStateContributor.class);
    ListAppender<ILoggingEvent> logCapture = new ListAppender<>();
    logCapture.start();
    contributorLogger.addAppender(logCapture);
    try {
      // getState() is polled repeatedly by every authenticated UI session; simulate several consecutive polls.
      Map<String, Object> state = null;
      for (int i = 0; i < 5; i++) {
        state = underTest.getState();
        // No crash, and the default role is simply omitted from state on every poll.
        assertThat(state, is(emptyMap()));
      }

      // Despite five polls of the same missing role, only a SINGLE visible WARN is emitted (NEXUS-53915: no spam),
      // and it is catchable by ERROR/WARN log filters.
      List<ILoggingEvent> warnings =
          logCapture.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
      assertThat(warnings.size(), is(1));
      assertThat(warnings.get(0).getFormattedMessage(), containsString("deleted-default-role"));

      // Echo the captured line so it is visible in the test run output.
      warnings.forEach(event -> System.out.println(
          "CAPTURED LOG >>> " + event.getLevel() + " " + event.getLoggerName() + " - " + event.getFormattedMessage()));
    }
    finally {
      contributorLogger.detachAppender(logCapture);
    }
  }

  /**
   * NEXUS-53915: the missing-role WARN is de-duplicated per condition, not silenced forever. If the configuration
   * recovers (role restored) and then breaks again, a fresh WARN must be emitted.
   */
  @Test
  public void missingDefaultRoleWarningReArmsAfterRecovery() throws NoSuchAuthorizationManagerException {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    Subject subject = mock(Subject.class);
    when(subject.isAuthenticated()).thenReturn(true);
    setSubject(subject);

    Role otherRole = new Role();
    otherRole.setRoleId("some-other-role");
    otherRole.setName("Some Other Role");

    // Poll sequence: missing (WARN #1) -> still missing (silent) -> role restored (re-arm) -> missing again (WARN #2).
    when(securitySystem.listRoles(DEFAULT_SOURCE))
        .thenReturn(singleton(otherRole))
        .thenReturn(singleton(otherRole))
        .thenReturn(singleton(defaultRole))
        .thenReturn(singleton(otherRole));

    Logger contributorLogger = (Logger) LoggerFactory.getLogger(DefaultRoleStateContributor.class);
    ListAppender<ILoggingEvent> logCapture = new ListAppender<>();
    logCapture.start();
    contributorLogger.addAppender(logCapture);
    try {
      underTest.getState();
      underTest.getState();
      underTest.getState();
      underTest.getState();

      // Exactly two WARNs: one per distinct breakage, proving de-duplication does not permanently silence the signal.
      List<ILoggingEvent> warnings =
          logCapture.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
      assertThat(warnings.size(), is(2));
    }
    finally {
      contributorLogger.detachAppender(logCapture);
    }
  }

  @Test
  public void authenticatedUserDoesNotGetTheDefaultRoleWhenDisabled() {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(false);
    Subject subject = mock(Subject.class);
    when(subject.isAuthenticated()).thenReturn(true);
    setSubject(subject);

    Map<String, Object> state = underTest.getState();

    assertThat(state, is(emptyMap()));
  }

  private void setSubject(Subject subject) {
    assert subjectThreadState == null;

    subjectThreadState = new SubjectThreadState(subject);
    subjectThreadState.bind();
  }

}
