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

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @since 3.next
 */
public class ApiCreateUser
{
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.USER_ID_DESCRIPTION)
  private String userId;

  @NotEmpty
  @ApiModelProperty(NexusSecurityApiConstants.FIRST_NAME_DESCRIPTION)
  private String firstName;

  @NotEmpty
  @ApiModelProperty(NexusSecurityApiConstants.LAST_NAME_DESCRIPTION)
  private String lastName;

  @Email
  @NotEmpty
  @ApiModelProperty(NexusSecurityApiConstants.EMAIL_DESCRIPTION)
  private String emailAddress;

  @NotEmpty
  @ApiModelProperty("The password for the new user.")
  private String password;

  @NotNull
  @ApiModelProperty(NexusSecurityApiConstants.STATUS_DESCRIPTION)
  private ApiUserStatus status;

  @NotEmpty
  @ApiModelProperty(NexusSecurityApiConstants.ROLES_DESCRIPTION)
  private Set<String> roles;

  @SuppressWarnings("unused")
  private ApiCreateUser() {
    // for deserialization
  }

  ApiCreateUser(
      final String userId,
      final String firstName,
      final String lastName,
      final String emailAddress,
      final String password,
      final ApiUserStatus status,
      final Set<String> roles)
  {
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.emailAddress = emailAddress;
    this.password = password;
    this.status = status;
    this.roles = roles;
  }

  public String getUserId() {
    return userId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public ApiUserStatus getStatus() {
    return status;
  }

  public String getPassword() {
    return password;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public void setEmailAddress(final String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public void setStatus(final ApiUserStatus status) {
    this.status = status;
  }

  public void setRoles(final Set<String> roles) {
    this.roles = roles;
  }

  User toUser() {
    User user = new User();
    user.setUserId(userId);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmailAddress(emailAddress);
    user.setStatus(status.getStatus());
    user.setReadOnly(false);
    user.setVersion("1");
    user.setSource(UserManager.DEFAULT_SOURCE);
    user.setRoles(roles.stream().map(r -> new RoleIdentifier(UserManager.DEFAULT_SOURCE, r))
        .collect(Collectors.toSet()));
    return user;
  }
}
