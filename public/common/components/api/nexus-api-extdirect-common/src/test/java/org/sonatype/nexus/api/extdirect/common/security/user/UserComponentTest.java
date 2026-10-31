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
package org.sonatype.nexus.api.extdirect.common.security.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.api.common.shared.security.model.ReferenceXO;
import org.sonatype.nexus.api.extdirect.common.security.user.model.UserRoleMappingsXO;
import org.sonatype.nexus.api.extdirect.common.security.user.model.UserXO;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.realm.RealmExistsValidator;
import org.sonatype.nexus.security.role.RolesExistValidator;
import org.sonatype.nexus.security.user.UniqueUserIdValidator;
import org.sonatype.nexus.security.user.UserExistsValidator;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.RegisterValidator;

import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UserComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@ExtendWith(ValidationExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@WithUser
class UserComponentTest
{
  private static final String DEFAULT_SOURCE = UserManager.DEFAULT_SOURCE;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private AuthTicketService authTickets;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AnonymousConfiguration anonymousConfiguration;

  @Mock
  private Subject subject;

  @RegisterValidator
  @Mock
  private UniqueUserIdValidator uniqueUserIdValidator;

  @RegisterValidator
  @Mock
  private RolesExistValidator rolesExistValidator;

  @RegisterValidator
  @Mock
  private UserExistsValidator userExistsValidator;

  @RegisterValidator
  @Mock
  private RealmExistsValidator realmExistsValidator;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Captor
  private ArgumentCaptor<UserSearchCriteria> criteriaCaptor;

  @Captor
  private ArgumentCaptor<Set<RoleIdentifier>> rolesCaptor;

  private UserComponent underTest;

  @BeforeEach
  void allowCustomConstraintValidation() {
    when(uniqueUserIdValidator.isValid(any(), any())).thenReturn(true);
    when(rolesExistValidator.isValid(any(), any())).thenReturn(true);
    when(userExistsValidator.isValid(any(), any())).thenReturn(true);
    when(realmExistsValidator.isValid(any(), any())).thenReturn(true);
  }

  private UserComponent newComponent() {
    UserComponent component = new UserComponent(securitySystem, anonymousManager, authTickets);
    component.setApplicationContext(applicationContext);
    return component;
  }

  private static User user(
      final String userId,
      final String source,
      final int version,
      final Set<RoleIdentifier> roles)
  {
    User user = new User();
    user.setUserId(userId);
    user.setSource(source);
    user.setVersion(version);
    user.setFirstName("First-" + userId);
    user.setLastName("Last-" + userId);
    user.setEmailAddress(userId + "@example.com");
    user.setStatus(UserStatus.active);
    user.setRoles(roles);
    return user;
  }

  private static RoleIdentifier role(final String source, final String roleId) {
    return new RoleIdentifier(source, roleId);
  }

  // -------------------------------------------------------------------------------------- get / convert

  @Test
  void getConvertsInternalUser() throws Exception {
    underTest = newComponent();
    User user = user("jdoe", DEFAULT_SOURCE, 7, Set.of(role(DEFAULT_SOURCE, "nx-admin")));
    when(securitySystem.getUser("jdoe", DEFAULT_SOURCE)).thenReturn(user);

    UserXO xo = underTest.get("jdoe", DEFAULT_SOURCE);

    assertThat(xo.getUserId(), is("jdoe"));
    assertThat(xo.getVersion(), is("7"));
    assertThat(xo.getRealm(), is(DEFAULT_SOURCE));
    assertThat(xo.getFirstName(), is("First-jdoe"));
    assertThat(xo.getLastName(), is("Last-jdoe"));
    assertThat(xo.getEmail(), is("jdoe@example.com"));
    assertThat(xo.getStatus(), is(UserStatus.active));
    assertThat(xo.getPassword(), is(PasswordPlaceholder.get()));
    assertThat(xo.getRoles(), containsInAnyOrder("nx-admin"));
    assertThat(xo.isExternal(), is(false));
    assertThat(xo.getExternalRoles(), is(nullValue()));
  }

  @Test
  void getMarksExternalUserAndExtractsExternalRoles() throws Exception {
    underTest = newComponent();
    User user = user("ext", "ldap", 1, Set.of(role("ldap", "ldap-admin"), role(DEFAULT_SOURCE, "nx-anonymous")));
    when(securitySystem.getUser("ext", "ldap")).thenReturn(user);

    UserXO xo = underTest.get("ext", "ldap");

    assertThat(xo.getRealm(), is("ldap"));
    assertThat(xo.isExternal(), is(true));
    assertThat(xo.getRoles(), containsInAnyOrder("ldap-admin", "nx-anonymous"));
    assertThat(xo.getExternalRoles(), containsInAnyOrder("ldap-admin"));
  }

  // -------------------------------------------------------------------------------------- read

  @Test
  void readUsesFilterValuesFromParameters() {
    underTest = newComponent();
    StoreLoadParameters parameters = mock(StoreLoadParameters.class);
    when(parameters.getFilter("source")).thenReturn("ldap");
    when(parameters.getFilter("userId")).thenReturn("jdoe");
    when(parameters.getLimit()).thenReturn(25);
    when(securitySystem.searchUsers(any(UserSearchCriteria.class)))
        .thenReturn(Set.of(user("jdoe", "ldap", 1, Set.of())));

    List<UserXO> result = underTest.read(parameters);

    verify(securitySystem).searchUsers(criteriaCaptor.capture());
    UserSearchCriteria criteria = criteriaCaptor.getValue();
    assertThat(criteria.getSource(), is("ldap"));
    assertThat(criteria.getUserId(), is("jdoe"));
    assertThat(criteria.getLimit(), is(25));
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getUserId(), is("jdoe"));
  }

  @Test
  void readWithNullParametersUsesDefaults() {
    underTest = newComponent();
    when(securitySystem.searchUsers(any(UserSearchCriteria.class))).thenReturn(Set.of());

    List<UserXO> result = underTest.read(null);

    verify(securitySystem).searchUsers(criteriaCaptor.capture());
    UserSearchCriteria criteria = criteriaCaptor.getValue();
    assertThat(criteria.getSource(), is(DEFAULT_SOURCE));
    assertThat(criteria.getUserId(), is(nullValue()));
    assertThat(criteria.getLimit(), is(nullValue()));
    assertThat(result, hasSize(0));
  }

  // -------------------------------------------------------------------------------------- readSources

  @Test
  void readSourcesMapsUserManagersToReferences() {
    underTest = newComponent();
    UserManager userManager = mock(UserManager.class);
    when(applicationContext.getBeansOfType(UserManager.class)).thenReturn(Map.of("default", userManager));

    List<ReferenceXO> result = underTest.readSources();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getId(), is(notNullValue()));
    assertThat(result.get(0).getName(), is(result.get(0).getId()));
  }

  // -------------------------------------------------------------------------------------- create

  @Test
  void createBuildsUserOnDefaultSourceWithMappedRoles() throws Exception {
    underTest = newComponent();
    UserXO input = new UserXO();
    input.setUserId("newuser");
    input.setFirstName("New");
    input.setLastName("User");
    input.setEmail("new@example.com");
    input.setStatus(UserStatus.active);
    input.setPassword("s3cret");
    input.setRoles(Set.of("nx-admin"));

    User persisted = user("newuser", DEFAULT_SOURCE, 1, Set.of(role(DEFAULT_SOURCE, "nx-admin")));
    when(securitySystem.addUser(any(User.class), eq("s3cret"))).thenReturn(persisted);

    UserXO result = underTest.create(input);

    verify(securitySystem).addUser(userCaptor.capture(), eq("s3cret"));
    User sent = userCaptor.getValue();
    assertThat(sent.getUserId(), is("newuser"));
    assertThat(sent.getSource(), is(DEFAULT_SOURCE));
    assertThat(sent.getStatus(), is(UserStatus.active));
    assertThat(sent.getRoles(), containsInAnyOrder(role(DEFAULT_SOURCE, "nx-admin")));
    assertThat(result.getUserId(), is("newuser"));
    assertThat(result.getPassword(), is(PasswordPlaceholder.get()));
  }

  // -------------------------------------------------------------------------------------- update

  @Test
  void updateParsesVersionAndPersistsOnDefaultSource() throws Exception {
    underTest = newComponent();
    UserXO input = new UserXO();
    input.setUserId("edituser");
    input.setVersion("4");
    input.setFirstName("Edit");
    input.setLastName("User");
    input.setEmail("edit@example.com");
    input.setStatus(UserStatus.active);
    input.setRoles(Set.of("role-a", "role-b"));

    when(securitySystem.updateUser(any(User.class)))
        .thenReturn(user("edituser", DEFAULT_SOURCE, 5, Set.of()));

    underTest.update(input);

    verify(securitySystem).updateUser(userCaptor.capture());
    User sent = userCaptor.getValue();
    assertThat(sent.getUserId(), is("edituser"));
    assertThat(sent.getVersion(), is(4));
    assertThat(sent.getSource(), is(DEFAULT_SOURCE));
    assertThat(sent.getRoles(), containsInAnyOrder(
        role(DEFAULT_SOURCE, "role-a"), role(DEFAULT_SOURCE, "role-b")));
  }

  // -------------------------------------------------------------------------------------- updateRoleMappings

  @Test
  void updateRoleMappingsPersistsNonDefaultSourceRoles() throws Exception {
    underTest = newComponent();
    UserRoleMappingsXO mappings = new UserRoleMappingsXO();
    mappings.setUserId("jdoe");
    mappings.setRealm("ldap");
    mappings.setRoles(new LinkedHashSet<>(Set.of("keep-me", "already-ldap")));

    // Existing user already has "already-ldap" sourced from the same realm; it must be dropped from the update.
    User existing = user("jdoe", "ldap", 1, Set.of(role("ldap", "already-ldap")));
    when(securitySystem.getUser("jdoe", "ldap")).thenReturn(existing);

    underTest.updateRoleMappings(mappings);

    verify(securitySystem).setUsersRoles(eq("jdoe"), eq("ldap"), rolesCaptor.capture());
    Set<RoleIdentifier> sentRoles = rolesCaptor.getValue();
    assertThat(sentRoles, containsInAnyOrder(role(DEFAULT_SOURCE, "keep-me")));
  }

  @Test
  void updateRoleMappingsWithNoRolesClearsAssignments() throws Exception {
    underTest = newComponent();
    UserRoleMappingsXO mappings = new UserRoleMappingsXO();
    mappings.setUserId("jdoe");
    mappings.setRealm("ldap");
    mappings.setRoles(Set.of());
    when(securitySystem.getUser("jdoe", "ldap")).thenReturn(user("jdoe", "ldap", 1, Set.of()));

    underTest.updateRoleMappings(mappings);

    verify(securitySystem).setUsersRoles(eq("jdoe"), eq("ldap"), isNull());
  }

  // -------------------------------------------------------------------------------------- changePassword

  @Test
  void changePasswordSucceedsForValidTicketAndNonAnonymousUser() throws Exception {
    underTest = newComponent();
    when(authTickets.redeemTicket("ticket")).thenReturn(true);
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);

    underTest.changePassword("ticket", "jdoe", "newpass");

    verify(securitySystem).changePassword("jdoe", "newpass");
  }

  @Test
  void changePasswordRejectsAnonymousUser() throws Exception {
    underTest = newComponent();
    when(authTickets.redeemTicket("ticket")).thenReturn(true);
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(true);
    when(anonymousConfiguration.getUserId()).thenReturn("anonymous");

    assertThrows(Exception.class, () -> underTest.changePassword("ticket", "anonymous", "newpass"));
    verify(securitySystem, never()).changePassword(any(), any());
  }

  @Test
  void changePasswordRejectsInvalidTicket() throws Exception {
    underTest = newComponent();
    when(authTickets.redeemTicket("bad")).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> underTest.changePassword("bad", "jdoe", "newpass"));
    verify(securitySystem, never()).changePassword(any(), any());
  }

  // -------------------------------------------------------------------------------------- remove

  @Test
  void removeDeletesUser() throws Exception {
    underTest = newComponent();
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);
    when(securitySystem.getSubject()).thenReturn(subject);
    when(subject.getPrincipal()).thenReturn("someone-else");

    underTest.remove("jdoe", DEFAULT_SOURCE);

    verify(securitySystem).deleteUser("jdoe", DEFAULT_SOURCE);
  }

  @Test
  void removeRejectsAnonymousUser() throws Exception {
    underTest = newComponent();
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(true);
    when(anonymousConfiguration.getUserId()).thenReturn("anonymous");

    assertThrows(Exception.class, () -> underTest.remove("anonymous", DEFAULT_SOURCE));
    verify(securitySystem, never()).deleteUser(any(), any());
  }

  @Test
  void removeRejectsCurrentUser() throws Exception {
    underTest = newComponent();
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);
    when(securitySystem.getSubject()).thenReturn(subject);
    when(subject.getPrincipal()).thenReturn("jdoe");

    assertThrows(Exception.class, () -> underTest.remove("jdoe", DEFAULT_SOURCE));
    verify(securitySystem, never()).deleteUser(any(), any());
  }
}
