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
package org.sonatype.nexus.api.rest.selfhosted.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response.Status;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.api.rest.selfhosted.user.model.ApiCreateUser;
import org.sonatype.nexus.security.internal.rest.ApiUser;
import org.sonatype.nexus.security.internal.rest.ApiUserStatus;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class UserApiResourceTest
{
  public static final String USER_ID = "jsmith";

  @ValidationExecutor
  private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AdminPasswordFileManager adminPasswordFileManager;

  @Mock
  private UserManager userManager;

  private UserApiResource underTest;

  @BeforeEach
  void setup() throws Exception {
    underTest = new UserApiResource(securitySystem, adminPasswordFileManager);

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

  @AfterEach
  void cleanup() {
    adminPasswordFileManager.removeFile();
  }

  /*
   * Create user
   */
  @Test
  void testCreateUser() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenReturn(user);
    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    ApiUser returned = underTest.createUser(createUser);

    assertThat(returned, samePropertyValuesAs(underTest.fromUser(user)));

    verify(securitySystem).addUser(user, "admin123");
  }

  @Test
  void testCreateUser_missingUserManager() throws Exception {
    User user = createUser();
    when(securitySystem.addUser(user, "admin123")).thenThrow(new NoSuchUserManagerException(user.getSource()));

    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, Collections.singleton("nx-admin"));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.createUser(createUser), "Unable to locate source: default");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  /*
   * Update user
   */
  @Test
  void testUpdateUser() throws Exception {
    User user = createUser();
    underTest.updateUser(USER_ID, underTest.fromUser(user));

    verify(securitySystem).updateUser(user);
  }

  @Test
  void testUpdateUser_nullExternal() throws Exception {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    apiUser.setExternalRoles(null);

    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).updateUser(user);
  }

  @Test
  void testUpdateUser_externalSource() throws Exception {
    User user = createUser();
    user.setSource("LDAP");
    ApiUser apiUser = underTest.fromUser(user);
    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).setUsersRoles(USER_ID, "LDAP", user.getRoles());
  }

  @Test
  void testUpdateUser_externalSource_unknownUser() {
    User user = createUser();

    ApiUser apiUser = new ApiUser("jdoe", user.getFirstName(), user.getLastName(), user.getEmailAddress(),
        "LDAP", ApiUserStatus.convert(user.getStatus()), true, Collections.singleton("nx-admin"),
        Collections.emptySet());

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser("jdoe", apiUser), "User 'jdoe' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testUpdateUser_mismatch() {
    User user = createUser();
    ApiUser apiUser = underTest.fromUser(user);
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser("fred", apiUser), "The path's userId does not match the body");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
  }

  /**
   * Verifies that LDAP users can be updated without lastName.
   * Previously this would fail validation, but NEXUS-53662 fixed this
   * to align with Swagger spec which only marks status as required.
   */
  @Test
  void testUpdateUser_externalSource_withoutLastName() throws Exception {
    ApiUser apiUser = new ApiUser(USER_ID, "John", null, "john@example.org",
        "LDAP", ApiUserStatus.disabled, false, Collections.singleton("nx-admin"), Collections.emptySet());

    underTest.updateUser(USER_ID, apiUser);

    verify(securitySystem).setUsersRoles(USER_ID, "LDAP",
        apiUser.getRoles()
            .stream()
            .map(r -> new RoleIdentifier(UserManager.DEFAULT_SOURCE, r))
            .collect(Collectors.toSet()));
  }

  /**
   * Verifies that local users can be updated without lastName.
   * The field is now optional to match Swagger specification.
   */
  @Test
  void testUpdateUser_localSource_withoutLastName() throws Exception {
    ApiUser apiUser = new ApiUser(USER_ID, "John", null, "john@example.org",
        UserManager.DEFAULT_SOURCE, ApiUserStatus.disabled, false, Collections.singleton("nx-admin"), null);

    underTest.updateUser(USER_ID, apiUser);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(securitySystem).updateUser(captor.capture());
    assertNull(captor.getValue().getLastName());
    assertEquals("John", captor.getValue().getFirstName());
    assertEquals("john@example.org", captor.getValue().getEmailAddress());
  }

  /**
   * Verifies that local users can be updated without firstName.
   * The field is now optional to match Swagger specification.
   */
  @Test
  void testUpdateUser_localSource_withoutFirstName() throws Exception {
    ApiUser apiUser = new ApiUser(USER_ID, null, "Smith", "john@example.org",
        UserManager.DEFAULT_SOURCE, ApiUserStatus.disabled, false, Collections.singleton("nx-admin"), null);

    underTest.updateUser(USER_ID, apiUser);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(securitySystem).updateUser(captor.capture());
    assertNull(captor.getValue().getFirstName());
    assertEquals("Smith", captor.getValue().getLastName());
    assertEquals("john@example.org", captor.getValue().getEmailAddress());
  }

  /**
   * Verifies that local users can be updated without emailAddress.
   * The field is now optional to match Swagger specification.
   */
  @Test
  void testUpdateUser_localSource_withoutEmailAddress() throws Exception {
    ApiUser apiUser = new ApiUser(USER_ID, "John", "Smith", null,
        UserManager.DEFAULT_SOURCE, ApiUserStatus.disabled, false, Collections.singleton("nx-admin"), null);

    underTest.updateUser(USER_ID, apiUser);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(securitySystem).updateUser(captor.capture());
    assertNull(captor.getValue().getEmailAddress());
    assertEquals("John", captor.getValue().getFirstName());
    assertEquals("Smith", captor.getValue().getLastName());
  }

  @Test
  void testUpdateUser_unknownSource() throws Exception {
    User user = createUser();

    when(securitySystem.updateUser(user)).thenThrow(new NoSuchUserManagerException(user.getSource()));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser(USER_ID, underTest.fromUser(user)), "Unable to locate source: " + user.getSource());
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testUpdateUser_unknownUser() throws Exception {
    User user = createUser();

    when(securitySystem.updateUser(user)).thenThrow(new UserNotFoundException(user.getUserId()));

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.updateUser(USER_ID, underTest.fromUser(user)),
        "User '%s' not found.".formatted(user.getUserId()));
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  /*
   * Change password
   */

  @Test
  void testChangePassword() throws Exception {
    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
  }

  @Test
  void testChangePassword_invalidUser() throws Exception {
    doThrow(new UserNotFoundException("test")).when(securitySystem).changePassword("test", "test");

    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.changePassword("test", "test"), "User 'test' not found.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.NOT_FOUND));
  }

  @Test
  void testChangePassword_missingPassword() throws Exception {
    Exception ex = assertThrows(Exception.class,
        () -> underTest.changePassword("test", null), "Password must be supplied.");

    if (ex instanceof WebApplicationMessageException webApplicationMessageException) {
      ValidationErrorXO error =
          assertInstanceOf(ValidationErrorXO.class, webApplicationMessageException.getResponse().getEntity());
      assertEquals("\"Password must be supplied.\"", error.getMessage());
    }
    else if (ex instanceof ConstraintViolationException constraintViolationException) {
      assertEquals(1, constraintViolationException.getConstraintViolations().size());

      constraintViolationException.getConstraintViolations()
          .stream()
          .findAny()
          .ifPresentOrElse(
              violation -> assertEquals("Password must be supplied.", violation.getMessage()),
              () -> fail("Expected ConstraintViolationException with message 'Password must be supplied.'"));
    }

    verify(securitySystem, never()).changePassword(any(), any());
  }

  @Test
  void testChangePassword_emptyPassword() throws Exception {
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.changePassword("test", ""), "Password must be supplied.");
    assertThat(ex.getResponse().getStatusInfo(), is(Status.BAD_REQUEST));
    verify(securitySystem, never()).changePassword(any(), any());
  }

  @Test
  void testChangePassword_defaultAdminRemoval() {
    underTest.changePassword("admin", "newPassword");

    verify(adminPasswordFileManager).removeFile();
  }

  @Test
  void testChangePassword_defaultAdminNotRemoved() throws Exception {
    underTest.changePassword("test", "test");

    verify(securitySystem).changePassword("test", "test");
    verify(adminPasswordFileManager, never()).removeFile();
  }

  /*
   * validateRoles
   */
  @Test
  void testCreateUser_nullRoleIdIsRejected() {
    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, new HashSet<>(Arrays.asList("nx-admin", null)));

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.createUser(createUser));
    assertThat(ex.getValidationErrors().stream().anyMatch(e -> e.getId().equals("roles")), is(true));
  }

  @Test
  void testCreateUser_blankRoleIdIsRejected() {
    ApiCreateUser createUser = new ApiCreateUser(USER_ID, "John", "Smith", "jsmith@example.org", "admin123",
        ApiUserStatus.disabled, new HashSet<>(Arrays.asList("nx-admin", "   ")));

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.createUser(createUser));
    assertThat(ex.getValidationErrors().stream().anyMatch(e -> e.getId().equals("roles")), is(true));
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
}
