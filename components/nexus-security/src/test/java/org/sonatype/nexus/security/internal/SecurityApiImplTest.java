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
package org.sonatype.nexus.security.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.TestAnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 3.0
 */
public class SecurityApiImplTest
    extends TestSupport
{
  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  private TestAnonymousConfiguration configuration = new TestAnonymousConfiguration();

  @InjectMocks
  private SecurityApiImpl api;

  @Before
  public void setup() throws NoSuchAuthorizationManagerException {
    when(securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)).thenReturn(authorizationManager);
    when(anonymousManager.getConfiguration()).thenReturn(configuration);
  }

  @Test
  public void testSetAnonymousAccess() {
    configuration.setEnabled(true);

    AnonymousConfiguration updatedConfiguration = api.setAnonymousAccess(false);

    assertFalse(updatedConfiguration.isEnabled());
    verify(anonymousManager).getConfiguration();
    verify(anonymousManager).setConfiguration(any());
  }

  /*
   * No save is made when configured and anonymous settings already match
   */
  @Test
  public void testSetAnonymousAccess_unchanged() {
    configuration.setEnabled(true);
    when(anonymousManager.isConfigured()).thenReturn(true);

    AnonymousConfiguration updatedConfiguration = api.setAnonymousAccess(true);

    assertTrue(updatedConfiguration.isEnabled());
    verify(anonymousManager).getConfiguration();
    verify(anonymousManager, never()).setConfiguration(any());
  }

  /*
   * One save is made when unconfigured and anonymous settings already match
   */
  @Test
  public void testSetAnonymousAccess_unconfigured() {
    when(anonymousManager.isConfigured()).thenReturn(false);

    AnonymousConfiguration updatedConfiguration = api.setAnonymousAccess(false);

    assertFalse(updatedConfiguration.isEnabled());

    verify(anonymousManager).getConfiguration();
    verify(anonymousManager).setConfiguration(any());
  }

  @Test
  public void testAddUser() throws NoSuchUserManagerException {
    when(securitySystem.addUser(any(), eq("pass"))).thenAnswer(i -> i.getArguments()[0]);

    User user = api.addUser("foo", "bar", "baz", "foo@bar.com", true, "pass", List.of("roleId"));

    verify(securitySystem).addUser(any(), eq("pass"));

    assertThat(user.getUserId(), is("foo"));
    assertThat(user.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertThat(user.getFirstName(), is("bar"));
    assertThat(user.getLastName(), is("baz"));
    assertThat(user.getEmailAddress(), is("foo@bar.com"));
    assertThat(user.getStatus(), is(UserStatus.active));
    assertThat(user.getRoles(), contains(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "roleId")));
  }

  @Test
  public void testAddRole() throws NoSuchAuthorizationManagerException {
    when(authorizationManager.addRole(any())).thenAnswer(i -> i.getArguments()[0]);

    Role role = api.addRole("foo", "bar", "baz", List.of("priv"), List.of("role"));

    verify(securitySystem).getAuthorizationManager(any());
    verify(authorizationManager).addRole(any());

    assertThat(role.getRoleId(), is("foo"));
    assertThat(role.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertThat(role.getName(), is("bar"));
    assertThat(role.getDescription(), is("baz"));
    assertThat(role.getPrivileges(), contains("priv"));
    assertThat(role.getRoles(), contains("role"));
  }
}
