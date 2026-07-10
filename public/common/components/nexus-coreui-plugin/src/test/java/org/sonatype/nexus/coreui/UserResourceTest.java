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
package org.sonatype.nexus.coreui;

import jakarta.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class UserResourceTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthTicketService authTickets;

  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private AnonymousConfiguration anonymousConfiguration;

  private UserResource underTest;

  @BeforeEach
  void setup() {
    underTest = new UserResource(securitySystem, authTickets, anonymousManager);
    lenient().when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
  }

  @Test
  void readAccount_returnsCurrentUser() throws Exception {
    User user = createUser("admin", "Admin", "User", "admin@example.com", "default");
    when(securitySystem.currentUser()).thenReturn(user);

    UserAccountXO result = underTest.readAccount();

    assertThat(result, is(notNullValue()));
    assertThat(result.getUserId(), is("admin"));
    assertThat(result.getFirstName(), is("Admin"));
    assertThat(result.getLastName(), is("User"));
    assertThat(result.getEmail(), is("admin@example.com"));
    assertThat(result.getExternal(), is(false));
  }

  @Test
  void readAccount_externalUser() throws Exception {
    User user = createUser("ldapuser", "LDAP", "User", "ldap@example.com", "LDAP");
    when(securitySystem.currentUser()).thenReturn(user);

    UserAccountXO result = underTest.readAccount();

    assertThat(result.getExternal(), is(true));
  }

  @Test
  void readAccount_returnsBadRequestWhenNoCurrentUser() throws Exception {
    when(securitySystem.currentUser()).thenReturn(null);

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.readAccount());
    assertThat(exception.getResponse().getStatus(), is(400));
  }

  @Test
  void readAccount_returnsBadRequestWhenCurrentUserThrowsUserNotFoundException() throws Exception {
    when(securitySystem.currentUser()).thenThrow(new UserNotFoundException("test-user"));

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.readAccount());
    assertThat(exception.getResponse().getStatus(), is(400));
  }

  @Test
  void updateAccount_updatesUser() throws Exception {
    User user = createUser("admin", "Admin", "User", "admin@example.com", "default");
    when(securitySystem.currentUser()).thenReturn(user);

    UserAccountXO xo = new UserAccountXO();
    xo.setUserId("admin");
    xo.setFirstName("Updated");
    xo.setLastName("Name");
    xo.setEmail("updated@example.com");

    underTest.updateAccount(xo);

    assertThat(user.getFirstName(), is("Updated"));
    assertThat(user.getLastName(), is("Name"));
    assertThat(user.getEmailAddress(), is("updated@example.com"));
    verify(securitySystem).updateUser(user);
  }

  @Test
  void updateAccount_throwsWhenUserIdMismatch() throws Exception {
    User user = createUser("admin", "Admin", "User", "admin@example.com", "default");
    when(securitySystem.currentUser()).thenReturn(user);

    UserAccountXO xo = new UserAccountXO();
    xo.setUserId("different-user");
    xo.setFirstName("Updated");
    xo.setLastName("Name");
    xo.setEmail("updated@example.com");

    assertThrows(WebApplicationMessageException.class, () -> underTest.updateAccount(xo));
    verify(securitySystem, never()).updateUser(user);
  }

  @Test
  void updateAccount_returnsBadRequestWhenNoCurrentUser() throws Exception {
    when(securitySystem.currentUser()).thenReturn(null);

    UserAccountXO xo = new UserAccountXO();
    xo.setUserId("admin");
    xo.setFirstName("Updated");
    xo.setLastName("Name");
    xo.setEmail("updated@example.com");

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.updateAccount(xo));
    assertThat(exception.getResponse().getStatus(), is(400));
  }

  @Test
  void changePassword_successful() throws Exception {
    when(authTickets.redeemTicket("valid-ticket")).thenReturn(true);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);

    UserAccountPasswordXO xo = new UserAccountPasswordXO();
    xo.setAuthToken("valid-ticket");
    xo.setPassword("newPassword123");

    underTest.changePassword("admin", xo);

    verify(securitySystem).changePassword("admin", "newPassword123");
  }

  @Test
  void changePassword_invalidTicket_throwsForbidden() throws Exception {
    when(authTickets.redeemTicket("invalid-ticket")).thenReturn(false);

    UserAccountPasswordXO xo = new UserAccountPasswordXO();
    xo.setAuthToken("invalid-ticket");
    xo.setPassword("newPassword123");

    assertThrows(WebApplicationMessageException.class, () -> underTest.changePassword("admin", xo));
    verify(securitySystem, never()).changePassword("admin", "newPassword123");
  }

  @Test
  void changePassword_anonymousUser_throwsBadRequest() throws Exception {
    when(authTickets.redeemTicket("valid-ticket")).thenReturn(true);
    when(anonymousConfiguration.isEnabled()).thenReturn(true);
    when(anonymousConfiguration.getUserId()).thenReturn("anonymous");

    UserAccountPasswordXO xo = new UserAccountPasswordXO();
    xo.setAuthToken("valid-ticket");
    xo.setPassword("newPassword123");

    assertThrows(WebApplicationMessageException.class, () -> underTest.changePassword("anonymous", xo));
    verify(securitySystem, never()).changePassword("anonymous", "newPassword123");
  }

  @Test
  void changePassword_nonAnonymousUser_whenAnonymousEnabled() throws Exception {
    when(authTickets.redeemTicket("valid-ticket")).thenReturn(true);
    when(anonymousConfiguration.isEnabled()).thenReturn(true);
    when(anonymousConfiguration.getUserId()).thenReturn("anonymous");

    UserAccountPasswordXO xo = new UserAccountPasswordXO();
    xo.setAuthToken("valid-ticket");
    xo.setPassword("newPassword123");

    underTest.changePassword("admin", xo);

    verify(securitySystem).changePassword("admin", "newPassword123");
  }

  @Test
  void changePassword_anonymousDisabled_allowsAnonymousUserId() throws Exception {
    when(authTickets.redeemTicket("valid-ticket")).thenReturn(true);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);

    UserAccountPasswordXO xo = new UserAccountPasswordXO();
    xo.setAuthToken("valid-ticket");
    xo.setPassword("newPassword123");

    underTest.changePassword("anonymous", xo);

    verify(securitySystem).changePassword("anonymous", "newPassword123");
  }

  @Test
  void convert_defaultSourceUser() {
    User user = createUser("testuser", "Test", "User", "test@example.com", "default");

    UserAccountXO result = underTest.convert(user);

    assertThat(result.getUserId(), is("testuser"));
    assertThat(result.getFirstName(), is("Test"));
    assertThat(result.getLastName(), is("User"));
    assertThat(result.getEmail(), is("test@example.com"));
    assertThat(result.getExternal(), is(false));
  }

  @Test
  void convert_externalSourceUser() {
    User user = createUser("ldapuser", "LDAP", "User", "ldap@example.com", "LDAP");

    UserAccountXO result = underTest.convert(user);

    assertThat(result.getExternal(), is(true));
  }

  private User createUser(
      final String userId,
      final String firstName,
      final String lastName,
      final String email,
      final String source)
  {
    User user = new User();
    user.setUserId(userId);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmailAddress(email);
    user.setSource(source);
    return user;
  }
}
