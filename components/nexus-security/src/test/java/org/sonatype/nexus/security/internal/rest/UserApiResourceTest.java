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
package org.sonatype.nexus.security.internal.rest;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.hamcrest.BeanMatchers;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserApiResourceTest
    extends TestSupport
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private UserManager userManager;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private UserApiResource underTest;

  @Before
  public void setup() throws Exception {
    underTest = new UserApiResource(securitySystem);

    final User user = createUser();
    when(securitySystem.getUser(any(), any())).thenAnswer(i -> {
      if ("jdoe".equals(i.getArguments()[0]) && "LDAP".equals(i.getArguments()[1])) {
        throw new UserNotFoundException((String) i.getArguments()[0]);
      }
      return user;
    });
    when(securitySystem.getUser(user.getUserId())).thenReturn(user);

    UserManager ldap = mock(UserManager.class);
    when(ldap.supportsWrite()).thenReturn(false);
    when(securitySystem.getUserManager("LDAP")).thenReturn(ldap);

    when(securitySystem.getUserManager(UserManager.DEFAULT_SOURCE)).thenReturn(userManager);
    when(securitySystem.listRoles(UserManager.DEFAULT_SOURCE))
        .thenReturn(Collections.singleton(new Role("nx-admin", null, null, null, true, null, null)));
    when(userManager.supportsWrite()).thenReturn(true);
  }

  /*
   * Get users
   */
  @Test
  public void testGetUsers() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));
    Collection<ApiUser> users = underTest.getUsers("js", UserManager.DEFAULT_SOURCE);

    assertThat(users, hasSize(1));
    assertThat(users, contains(BeanMatchers.similarTo(underTest.fromUser(createUser()))));

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertNull(criteria.getLimit());
  }

  @Test
  public void testGetUsers_nonDefaultLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", null);

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertNull(criteria.getSource());
    assertThat(criteria.getLimit(), is(100));
  }

  /*
   * Create user
   */
  @Test
  public void testCreateUser() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenReturn(user);
    ApiCreateUser createUser = new ApiCreateUser("jsmith", "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    ApiUser returned = underTest.createUser(createUser);

    assertThat(returned, BeanMatchers.similarTo(underTest.fromUser(user)));

    verify(securitySystem).addUser(user, "admin123");
  }

  @Test
  public void testCreateUser_missingUserManager() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenThrow(new NoSuchUserManagerException(user.getSource()));
    expectUnknownUserManager("default");

    ApiCreateUser createUser = new ApiCreateUser("jsmith", "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    underTest.createUser(createUser);
  }

  /*
   * Delete user
   */
  @Test
  public void testDeleteUsers() throws Exception {
    underTest.deleteUser("jsmith");

    verify(securitySystem).deleteUser("jsmith", UserManager.DEFAULT_SOURCE);
  }

  @Test
  public void testDeleteUsers_missingUser() throws Exception {
    when(securitySystem.getUser("unknownuser")).thenThrow(new UserNotFoundException("unknownuser"));
    expectMissingUser("unknownuser");

    underTest.deleteUser("unknownuser");
  }

  @Test
  public void testDeleteUsers_somethingWonky() throws Exception {
    User user = createUser();
    doThrow(new NoSuchUserManagerException(user.getSource())).when(securitySystem).deleteUser(user.getUserId(),
        user.getSource());
    expectUnknownUserManager(user.getSource());

    underTest.deleteUser("jsmith");
  }

  /*
   * Update user
   */
  @Test
  public void testUpdateUser() throws Exception {
    User user = createUser();
    underTest.updateUser("jsmith", underTest.fromUser(user));

    verify(securitySystem).updateUser(user);
  }

  @Test
  public void testUpdateUser_nullExternal() throws Exception {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    apiUser.setExternalRoles(null);

    underTest.updateUser("jsmith", apiUser);

    verify(securitySystem).updateUser(user);
  }

  @Test
  public void testUpdateUser_externalSource() throws Exception {
    User user = createUser();
    user.setSource("LDAP");
    ApiUser apiUser = underTest.fromUser(user);
    underTest.updateUser("jsmith", apiUser);

    verify(securitySystem).setUsersRoles("jsmith", "LDAP", user.getRoles());
  }

  @Test
  public void testUpdateUser_externalSource_unknownUser() throws Exception {
    User user = createUser();

    ApiUser apiUser = new ApiUser("jdoe", user.getFirstName(), user.getLastName(), user.getEmailAddress(),
        "LDAP", ApiUserStatus.convert(user.getStatus()), true, Collections.emptySet(), Collections.emptySet());

    thrown.expect(matchWeb(Status.NOT_FOUND, "User 'jdoe' not found."));

    underTest.updateUser("jdoe", apiUser);

  }

  @Test
  public void testUpdateUser_mismatch() throws Exception {
    User user = createUser();
    thrown.expect(matchWeb(Status.BAD_REQUEST, "The path's userId does not match the body"));

    underTest.updateUser("fred", underTest.fromUser(user));
  }

  @Test
  public void testUpdateUser_unknownSource() throws Exception {
    User user = createUser();
    expectUnknownUserManager(user.getSource());

    when(securitySystem.updateUser(user)).thenThrow(new NoSuchUserManagerException(user.getSource()));
    underTest.updateUser("jsmith", underTest.fromUser(user));
  }

  @Test
  public void testUpdateUser_unknownUser() throws Exception {
    User user = createUser();
    expectMissingUser(user.getUserId());

    when(securitySystem.updateUser(user)).thenThrow(new UserNotFoundException(user.getUserId()));
    underTest.updateUser("jsmith", underTest.fromUser(user));
  }

  /*
   * Change password
   */

  @Test
  public void testChangePassword() throws Exception {
    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
  }

  @Test
  public void testChangePassword_invalidUser() throws Exception {
    doThrow(new UserNotFoundException("test")).when(securitySystem).changePassword("test", "test");

    expectMissingUser("test");

    underTest.changePassword("test", "test");
  }

  @Test
  public void testChangePassword_missingPassword() throws Exception {
    thrown.expect(matchWeb(Status.BAD_REQUEST, "Password must be supplied."));
    try {
      underTest.changePassword("test", null);
    }
    finally {
      verify(securitySystem, never()).changePassword(any(), any());
    }
  }

  @Test
  public void testChangePassword_emptyPassword() throws Exception {
    thrown.expect(matchWeb(Status.BAD_REQUEST, "Password must be supplied."));
    try {
      underTest.changePassword("test", "");
    }
    finally {
      verify(securitySystem, never()).changePassword(any(), any());
    }
  }

  private void expectMissingUser(final String userId) {
    thrown.expect(matchWeb(Status.NOT_FOUND, "User '" + userId + "' not found."));
  }

  private void expectUnknownUserManager(final String source) {
    thrown.expect(matchWeb(Status.NOT_FOUND, "Unable to locate source: " + source));
  }

  private User createUser() {
    User user = new User();
    user.setEmailAddress("john@example.org");
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId("jsmith");
    user.setVersion("1");
    user.setSource(UserManager.DEFAULT_SOURCE);
    user.setRoles(Collections.singleton(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-admin")));
    return user;
  }

  private Matcher<WebApplicationMessageException> matchWeb(final Status status, final String message) {
    return new BaseMatcher<WebApplicationMessageException>()
    {
      @Override
      public boolean matches(final Object item) {
        if (item instanceof WebApplicationMessageException) {
          WebApplicationMessageException e = (WebApplicationMessageException) item;
          return e.getResponse().getStatus() == status.getStatusCode()
              && message.equals(e.getResponse().getEntity().toString())
              && MediaType.APPLICATION_JSON_TYPE.equals(e.getResponse().getMediaType());
        }
        return false;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("WebApplicationMessageException(" + status.getStatusCode() + "," + message + ")");
      }
    };
  }
}
