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
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.ErrorMessageUtil;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.internal.AdminPasswordFileManagerImpl;
import org.sonatype.nexus.security.internal.RealmToSource;
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
import org.junit.After;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserApiResourceTest
    extends TestSupport
{
  public static final String USER_ID = "jsmith";
  private static final String SAML_REALM_NAME = "SamlRealm";
  private static final String CROWD_REALM_NAME = "Crowd";
  private static final String LDAP_REALM_NAME = "LdapRealm";
  private static final String NEXUS_AUTHENTICATING_REALM_NAME = "NexusAuthenticatingRealm";

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private ApplicationDirectories applicationDirectories;

  private AdminPasswordFileManager adminPasswordFileManager;

  @Mock
  private UserManager userManager;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private UserApiResource underTest;

  @Before
  public void setup() throws Exception {
    when(applicationDirectories.getWorkDirectory()).thenReturn(util.createTempDir());
    adminPasswordFileManager = new AdminPasswordFileManagerImpl(applicationDirectories);
    underTest = new UserApiResource(securitySystem, adminPasswordFileManager);

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

  @After
  public void cleanup() {
    adminPasswordFileManager.removeFile();
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
    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
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

    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    underTest.createUser(createUser);
  }

  /*
   * Delete user
   */
  @Test
  public void testDeleteUsersWithNoRealm() throws Exception {
    underTest.deleteUser(USER_ID, null);

    verify(securitySystem).deleteUser(USER_ID, UserManager.DEFAULT_SOURCE);
  }

  @Test
  public void testDeleteUsersWithSamlRealm() throws Exception {
    when(securitySystem.isValidRealm(SAML_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(
        USER_ID,
        RealmToSource.getSource(SAML_REALM_NAME))).thenReturn(createUserWithSource(SAML_REALM_NAME));
    underTest.deleteUser(USER_ID, SAML_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(SAML_REALM_NAME));
  }

  @Test
  public void testDeleteUsersWithEmptyRealm() {
    expectEmptyOrInvalidRealm();
    underTest.deleteUser(USER_ID, "");
  }

  @Test
  public void testDeleteUsersWithInvalidRealm() {
    when(securitySystem.isValidRealm(any())).thenReturn(false);
    expectEmptyOrInvalidRealm();
    underTest.deleteUser(USER_ID, "InvalidRealm123");
  }

  @Test
  public void testDeleteUsersWithCrowdRealm() throws Exception {
    when(securitySystem.isValidRealm(CROWD_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(CROWD_REALM_NAME))).thenReturn(createUserWithSource(CROWD_REALM_NAME));
    underTest.deleteUser(USER_ID, CROWD_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(CROWD_REALM_NAME));
  }

  @Test
  public void testDeleteUsersWithLdapRealm() throws Exception {
    when(securitySystem.isValidRealm(LDAP_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(LDAP_REALM_NAME))).thenReturn(createUserWithSource(LDAP_REALM_NAME));
    underTest.deleteUser(USER_ID, LDAP_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(LDAP_REALM_NAME));
  }

  @Test
  public void testDeleteUsersWithDefaultRealm() throws Exception {
    when(securitySystem.isValidRealm(NEXUS_AUTHENTICATING_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(NEXUS_AUTHENTICATING_REALM_NAME))).thenReturn(createUserWithSource(NEXUS_AUTHENTICATING_REALM_NAME));
    underTest.deleteUser(USER_ID, NEXUS_AUTHENTICATING_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, "default");
  }

  @Test
  public void testDeleteUsers_missingUser() throws Exception {
    when(securitySystem.getUser("unknownuser")).thenThrow(new UserNotFoundException("unknownuser"));
    expectMissingUser("unknownuser");

    underTest.deleteUser("unknownuser", null);
  }

  @Test
  public void testDeleteUsers_somethingWonky() throws Exception {
    User user = createUser();
    doThrow(new NoSuchUserManagerException(user.getSource())).when(securitySystem).deleteUser(user.getUserId(),
        user.getSource());
    expectUnknownUserManager(user.getSource());

    underTest.deleteUser(USER_ID, null);
  }

  /*
   * Update user
   */
  @Test
  public void testUpdateUser() throws Exception {
    User user = createUser();
    underTest.updateUser(USER_ID, underTest.fromUser(user));

    verify(securitySystem).updateUser(user);
  }

  @Test
  public void testUpdateUser_nullExternal() throws Exception {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    apiUser.setExternalRoles(null);

    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).updateUser(user);
  }

  @Test
  public void testUpdateUser_externalSource() throws Exception {
    User user = createUser();
    user.setSource("LDAP");
    ApiUser apiUser = underTest.fromUser(user);
    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).setUsersRoles(USER_ID, "LDAP", user.getRoles());
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
  public void testDeleteLdapUser() throws Exception {
    when(securitySystem.getUser(any())).thenReturn(createLdapUser());
    thrown.expect(matchWeb(Status.BAD_REQUEST, "Non-local user cannot be deleted."));

    underTest.deleteUser("tanderson", null);
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
    underTest.updateUser(USER_ID, underTest.fromUser(user));
  }

  @Test
  public void testUpdateUser_unknownUser() throws Exception {
    User user = createUser();
    expectMissingUser(user.getUserId());

    when(securitySystem.updateUser(user)).thenThrow(new UserNotFoundException(user.getUserId()));
    underTest.updateUser(USER_ID, underTest.fromUser(user));
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

  @Test
  public void testChangePassword_defaultAdminRemoval() throws Exception {
    adminPasswordFileManager.writeFile("oldPassword");

    underTest.changePassword("admin", "newPassword");

    assertThat(adminPasswordFileManager.exists(), is(false));
  }

  @Test
  public void testChangePassword_defaultAdminNotRemoved() throws Exception {
    adminPasswordFileManager.writeFile("oldPassword");

    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
    assertThat(adminPasswordFileManager.exists(), is(true));
  }

  private void expectMissingUser(final String userId) {
    thrown.expect(matchWeb(Status.NOT_FOUND, "User '" + userId + "' not found."));
  }

  private void expectUnknownUserManager(final String source) {
    thrown.expect(matchWeb(Status.NOT_FOUND, "Unable to locate source: " + source));
  }

  private void expectEmptyOrInvalidRealm() {
    thrown.expect(matchWeb(Status.BAD_REQUEST, "Invalid or empty realm name."));
  }

  private User createUser() {
    User user = new User();
    user.setEmailAddress("john@example.org");
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId(USER_ID);
    user.setVersion(1);
    user.setSource(UserManager.DEFAULT_SOURCE);
    user.setRoles(Collections.singleton(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-admin")));
    return user;
  }

  private User createLdapUser() {
    User user = new User();
    user.setEmailAddress("thomas@example.org");
    user.setFirstName("Thomas");
    user.setLastName("Anderson");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId("tanderson");
    user.setVersion(1);
    user.setSource("LDAP");
    user.setRoles(Collections.singleton(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-admin")));
    return user;
  }

  private User createUserWithSource(String realm) {
    User user = new User();
    user.setEmailAddress("john@example.org");
    user.setFirstName("John");
    user.setLastName("Smith");
    user.setReadOnly(false);
    user.setStatus(UserStatus.disabled);
    user.setUserId(USER_ID);
    user.setVersion(1);
    user.setSource(RealmToSource.getSource(realm));
    user.setRoles(Collections.singleton(new RoleIdentifier(RealmToSource.getSource(realm), "nx-admin")));
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
              && (ErrorMessageUtil.getFormattedMessage("\"" + message + "\""))
                  .equals(e.getResponse().getEntity().toString())
              && MediaType.APPLICATION_JSON_TYPE.equals(e.getResponse().getMediaType());
        }
        return false;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("WebApplicationMessageException(" + status.getStatusCode() + ","
                + ErrorMessageUtil.getFormattedMessage(message) + ")");
      }
    };
  }
}
