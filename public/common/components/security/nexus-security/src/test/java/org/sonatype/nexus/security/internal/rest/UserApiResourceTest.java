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
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response.Status;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.internal.RealmToSource;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class UserApiResourceTest
{
  public static final String USER_ID = "jsmith";

  private static final String SAML_REALM_NAME = "SamlRealm";

  private static final String OAUTH_REALM_NAME = "OAuth2Realm";

  private static final String CROWD_REALM_NAME = "Crowd";

  private static final String LDAP_REALM_NAME = "LdapRealm";

  private static final String NEXUS_AUTHENTICATING_REALM_NAME = "NexusAuthenticatingRealm";

  @ValidationExecutor
  private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private UserManager userManager;

  private UserApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    underTest = new UserApiResource(securitySystem);

    final User user = createUser();
    lenient().when(securitySystem.getUser(any(), any())).thenAnswer(i -> {
      if ("jdoe".equals(i.getArguments()[0]) && "LDAP".equals(i.getArguments()[1])) {
        throw new UserNotFoundException((String) i.getArguments()[0]);
      }
      return user;
    });
    lenient().when(securitySystem.getUser(user.getUserId())).thenReturn(user);

    UserManager ldap = mock(UserManager.class);
    lenient().when(ldap.supportsWrite()).thenReturn(false);
    lenient().when(securitySystem.getUserManager("LDAP")).thenReturn(ldap);

    lenient().when(securitySystem.getUserManager(UserManager.DEFAULT_SOURCE)).thenReturn(userManager);
    lenient().when(securitySystem.listRoles(UserManager.DEFAULT_SOURCE))
        .thenReturn(Collections.singleton(new Role("nx-admin", null, null, null, true, null, null)));
    lenient().when(userManager.supportsWrite()).thenReturn(true);
  }

  /*
   * Get users
   */
  @Test
  void testGetUsers() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));
    Collection<ApiUser> users = underTest.getUsers("js", UserManager.DEFAULT_SOURCE);

    assertThat(users, hasSize(1));
    assertThat(users, contains(samePropertyValuesAs(underTest.fromUser(createUser()))));

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertNull(criteria.getLimit());
  }

  @Test
  void testGetUsers_nonDefaultLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", null);

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertNull(criteria.getSource());
    assertThat(criteria.getLimit(), is(100));
  }

  @Test
  void testGetUsers_samlSourceLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", UserManager.SAML_SOURCE);

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is(UserManager.SAML_SOURCE));
    assertThat(criteria.getLimit(), is(1000));
  }

  @Test
  void testGetUsers_ldapSourceLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", "LDAP");

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is("LDAP"));
    assertThat(criteria.getLimit(), is(100));
  }

  @Test
  void testGetUsers_defaultSourceNoLimit() {
    when(securitySystem.searchUsers(any())).thenReturn(Collections.singleton(createUser()));

    underTest.getUsers("js", UserManager.DEFAULT_SOURCE);

    ArgumentCaptor<UserSearchCriteria> captor = ArgumentCaptor.forClass(UserSearchCriteria.class);
    verify(securitySystem).searchUsers(captor.capture());

    UserSearchCriteria criteria = captor.getValue();
    assertThat(criteria.getUserId(), is("js"));
    assertThat(criteria.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertNull(criteria.getLimit());
  }

  /*
   * Delete user
   */
  @Test
  void testDeleteUsersWithNoRealm() throws Exception {
    underTest.deleteUser(USER_ID, null);

    verify(securitySystem).deleteUser(USER_ID, UserManager.DEFAULT_SOURCE);
  }

  @Test
  void testDeleteUsersWithSamlRealm() throws Exception {
    when(securitySystem.isValidRealm(SAML_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(
        USER_ID,
        RealmToSource.getSource(SAML_REALM_NAME))).thenReturn(createUserWithSource(SAML_REALM_NAME));
    underTest.deleteUser(USER_ID, SAML_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(SAML_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithOAuthRealm() throws Exception {
    when(securitySystem.isValidRealm(OAUTH_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(
        USER_ID,
        RealmToSource.getSource(OAUTH_REALM_NAME))).thenReturn(createUserWithSource(OAUTH_REALM_NAME));
    underTest.deleteUser(USER_ID, OAUTH_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(OAUTH_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithEmptyRealm() {
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, ""), "Invalid or empty realm name.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testDeleteUsersWithInvalidRealm() {
    when(securitySystem.isValidRealm(any())).thenReturn(false);

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, "InvalidRealm123"), "Invalid or empty realm name.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  @Test
  void testDeleteUsersWithCrowdRealm() throws Exception {
    when(securitySystem.isValidRealm(CROWD_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(CROWD_REALM_NAME))).thenReturn(createUserWithSource(CROWD_REALM_NAME));
    underTest.deleteUser(USER_ID, CROWD_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(CROWD_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithLdapRealm() throws Exception {
    when(securitySystem.isValidRealm(LDAP_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(LDAP_REALM_NAME))).thenReturn(createUserWithSource(LDAP_REALM_NAME));
    underTest.deleteUser(USER_ID, LDAP_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, RealmToSource.getSource(LDAP_REALM_NAME));
  }

  @Test
  void testDeleteUsersWithDefaultRealm() throws Exception {
    when(securitySystem.isValidRealm(NEXUS_AUTHENTICATING_REALM_NAME)).thenReturn(true);
    when(securitySystem.getUser(USER_ID,
        RealmToSource.getSource(NEXUS_AUTHENTICATING_REALM_NAME))).thenReturn(
            createUserWithSource(NEXUS_AUTHENTICATING_REALM_NAME));
    underTest.deleteUser(USER_ID, NEXUS_AUTHENTICATING_REALM_NAME);

    verify(securitySystem).deleteUser(USER_ID, "default");
  }

  @Test
  void testDeleteUsers_missingUser() throws Exception {
    when(securitySystem.getUser("unknownuser")).thenThrow(new UserNotFoundException("unknownuser"));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser("unknownuser", null), "User 'unknownuser' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testDeleteUsers_somethingWonky() throws Exception {
    User user = createUser();
    doThrow(new NoSuchUserManagerException(user.getSource())).when(securitySystem)
        .deleteUser(user.getUserId(),
            user.getSource());

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.deleteUser(USER_ID, null), "Unable to locate source: " + user.getSource());
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  private static User createUser() {
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

  private static User createUserWithSource(final String realm) {
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
}
